/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.ir.eraseTypeParameters
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

// A value that may not have been fully constructed yet. The ability to "roll back" code generation
// is useful for certain optimizations.
abstract class PromisedValue(val codegen: ExpressionCodegen, val type: Type, val irType: IrType) {
    // If this value is immaterial, construct an object on the top of the stack. This
    // must always be done before generating other values or emitting raw bytecode.
    open fun materializeAt(target: Type, irTarget: IrType, castForReified: Boolean) {
        val erasedSourceType = irType.eraseTypeParameters()
        val erasedTargetType = irTarget.eraseTypeParameters()
        val fromTypeRepresentation = erasedSourceType.getClass()!!.inlineClassRepresentation
        val toTypeRepresentation = erasedTargetType.getClass()!!.inlineClassRepresentation

        // Coerce inline classes
        if (fromTypeRepresentation != null || toTypeRepresentation != null) {
            val isFromTypeUnboxed = fromTypeRepresentation?.underlyingType?.let(typeMapper::mapType) == type
            val isToTypeUnboxed = toTypeRepresentation?.underlyingType?.let(typeMapper::mapType) == target

            when {
                isFromTypeUnboxed && !isToTypeUnboxed -> {
                    StackValue.boxInlineClass(erasedSourceType, mv, typeMapper)
                    return
                }

                !isFromTypeUnboxed && isToTypeUnboxed -> {
                    val irClass = codegen.irFunction.parentAsClass
                    if (irClass.isInline && irClass.symbol == irType.classifierOrNull && !irType.isNullable()) {
                        // Use getfield instead of unbox-impl inside inline classes
                        codegen.mv.getfield(
                            typeMapper.classInternalName(irClass),
                            irClass.inlineClassFieldName.asString(),
                            typeMapper.mapType(irType).descriptor
                        )
                    } else {
                        StackValue.unboxInlineClass(type, erasedTargetType, mv, typeMapper)
                    }
                    return
                }
            }
        }

        if (type != target || (castForReified && irType.anyTypeArgument { it.isReified })) {
            if (codegen.typeMapper.mapType(erasedSourceType) == type && codegen.typeMapper.mapType(erasedTargetType) == target) {
                // what we really want to know here is if `type` is subType of `target` and if `target` is from Java. Before we find
                // a better way to do this, in order to avoid introduce unexpected breaking changes, it is important to determine whether
                // `erasedSourceType` and `erasedTargetType` are appropriate for the calculation of `isSubType` and `isFromJava`
                val isSubType = erasedSourceType.isSubtypeOfClass(erasedTargetType.classOrNull!!)
                val isFromJava = (erasedTargetType.classifierOrFail.owner as IrDeclaration).isFromJava()

                // we might need to cast to the correct upper bound when irType is multiple upper bounds type parameter
                val coerceTarget = computeCoerceTarget(isSubType, erasedTargetType, default = target)
                StackValue.coerce(type, coerceTarget, mv, type == coerceTarget, isSubType && isFromJava)
            } else StackValue.coerce(type, target, mv, type == target, false)
        }
    }

    abstract fun discard()

    val mv: InstructionAdapter
        get() = codegen.mv

    val typeMapper: IrTypeMapper
        get() = codegen.typeMapper

    private fun IrType.isPackagePrivate(): Boolean =
        getClass()?.visibility?.delegate == JavaVisibilities.PackageVisibility

    private fun IrType.inDifferentPackage(): Boolean =
        getClass()!!.getPackageFragment()?.fqName != codegen.irFunction.getPackageFragment()?.fqName

    private fun computeCoerceTarget(
        isSubType: Boolean,
        erasedTargetType: IrType,
        default: Type
    ): Type {
        if (!isSubType && erasedTargetType.isPackagePrivate() && erasedTargetType.inDifferentPackage()) {
            val computed = irType.superTypes().firstOrNull {
                it.isSubtypeOfClass(erasedTargetType.classOrNull!!)
            }
            if (computed != null) return codegen.typeMapper.mapType(computed)
            else throw CastToNonAccessibleClassException()
        }
        return default
    }
}

fun IrType.anyTypeArgument(check: (IrTypeParameter) -> Boolean): Boolean {
    when {
        isTypeParameter() -> if (check((classifierOrNull as IrTypeParameterSymbol).owner)) return true
        this is IrSimpleType -> return arguments.any { it.typeOrNull?.anyTypeArgument(check) ?: false }
    }
    return false
}

// A value that *has* been fully constructed.
class MaterialValue(codegen: ExpressionCodegen, type: Type, irType: IrType) : PromisedValue(codegen, type, irType) {
    override fun discard() {
        if (type !== Type.VOID_TYPE)
            AsmUtil.pop(mv, type)
    }
}

// A value that can be branched on. JVM has certain branching instructions which can be used
// to optimize these.
abstract class BooleanValue(codegen: ExpressionCodegen) :
    PromisedValue(codegen, Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType) {
    abstract fun jumpIfFalse(target: Label)
    abstract fun jumpIfTrue(target: Label)

    override fun materializeAt(target: Type, irTarget: IrType, castForReified: Boolean) {
        val const0 = Label()
        val end = Label()
        jumpIfFalse(const0)
        mv.iconst(1)
        mv.goTo(end)
        mv.mark(const0)
        mv.iconst(0)
        mv.mark(end)
        if (Type.BOOLEAN_TYPE != target) {
            StackValue.coerce(Type.BOOLEAN_TYPE, target, mv)
        }
    }
}

class BooleanConstant(codegen: ExpressionCodegen, val value: Boolean) : BooleanValue(codegen) {
    override fun jumpIfFalse(target: Label) = if (value) Unit else mv.goTo(target)
    override fun jumpIfTrue(target: Label) = if (value) mv.goTo(target) else Unit
    override fun materializeAt(target: Type, irTarget: IrType, castForReified: Boolean) {
        mv.iconst(if (value) 1 else 0)
        if (Type.BOOLEAN_TYPE != target) {
            StackValue.coerce(Type.BOOLEAN_TYPE, target, mv)
        }
    }

    override fun discard() {}
}

fun PromisedValue.coerceToBoolean(): BooleanValue =
    when (this) {
        is BooleanValue -> this
        else -> object : BooleanValue(codegen) {
            override fun jumpIfFalse(target: Label) =
                this@coerceToBoolean.materializeAt(Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType, false).also { mv.ifeq(target) }

            override fun jumpIfTrue(target: Label) =
                this@coerceToBoolean.materializeAt(Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType, false).also { mv.ifne(target) }

            override fun discard() {
                this@coerceToBoolean.discard()
            }
        }
    }

fun PromisedValue.materializeAt(target: Type, irTarget: IrType) {
    materializeAt(target, irTarget, false)
}

fun PromisedValue.materializedAt(target: Type, irTarget: IrType, castForReified: Boolean = false): MaterialValue {
    materializeAt(target, irTarget, castForReified)
    return MaterialValue(codegen, target, irTarget)
}

fun PromisedValue.materialized(): MaterialValue =
    materializedAt(type, irType)

fun PromisedValue.materializedAt(irTarget: IrType): MaterialValue =
    materializedAt(typeMapper.mapType(irTarget), irTarget)

fun PromisedValue.materializedAtBoxed(irTarget: IrType): MaterialValue =
    materializedAt(typeMapper.boxType(irTarget), irTarget)

fun PromisedValue.materialize() {
    materializeAt(type, irType)
}

fun PromisedValue.materializeAt(irTarget: IrType) {
    materializeAt(typeMapper.mapType(irTarget), irTarget)
}

fun PromisedValue.materializeAtBoxed(irTarget: IrType) {
    materializeAt(typeMapper.boxType(irTarget), irTarget)
}

// A Non-materialized value of Unit type that is only materialized through coercion.
val ExpressionCodegen.unitValue: PromisedValue
    get() = MaterialValue(this, Type.VOID_TYPE, context.irBuiltIns.unitType)

val ExpressionCodegen.nullConstant: PromisedValue
    get() = object : PromisedValue(this, AsmTypes.OBJECT_TYPE, context.irBuiltIns.nothingNType) {
        override fun materializeAt(target: Type, irTarget: IrType, castForReified: Boolean) {
            mv.aconst(null)
        }

        override fun discard() {}
    }
