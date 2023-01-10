/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.inlineClassFieldName
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.isInline
import org.jetbrains.kotlin.ir.declarations.isSealedInline
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
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

        val upcastFromNoinlineSealedInlineClassChild =
            erasedSourceType.isNoinlineChildOfSealedInlineClass() && erasedTargetType.classOrNull != erasedSourceType.classOrNull

        val downcastToNoinlineSealedInlineClassChild =
            erasedTargetType.isNoinlineChildOfSealedInlineClass() && erasedTargetType.classOrNull != erasedSourceType.classOrNull

        // Coerce inline classes
        val isFromTypeUnboxed = mapUnboxedType(erasedSourceType) == type
        val isToTypeUnboxed = mapUnboxedType(erasedTargetType) == target

        if (upcastFromNoinlineSealedInlineClassChild) {
            StackValue.coerce(type, AsmTypes.OBJECT_TYPE, mv)
            if (!isToTypeUnboxed) {
                // Upcasts from sealed inline class children lead to boxing, if the source is
                // not the inline class
                val targetType = typeMapper.mapType(erasedSourceType.findTopSealedInlineSuperClass().defaultType.makeNullable())
                StackValue.boxInlineClass(AsmTypes.OBJECT_TYPE, targetType, false, mv)
            }
            return
        }
        if (downcastToNoinlineSealedInlineClassChild) {
            // Downcasts to sealed inline class children lead to unboxing, if the target is
            // not the inline class
            if (!isFromTypeUnboxed) {
                val sourceType = typeMapper.mapType(erasedTargetType.findTopSealedInlineSuperClass().defaultType.makeNullable())
                StackValue.unboxInlineClass(type, sourceType, AsmTypes.OBJECT_TYPE, false, mv)
                StackValue.coerce(AsmTypes.OBJECT_TYPE, target, mv)
            } else {
                StackValue.coerce(type, target, mv)
            }
            return
        }

        if (isFromTypeUnboxed && !isToTypeUnboxed) {
            if (irType.isInlineChildOfSealedInlineClass()) {
                val top = irType.findTopSealedInlineSuperClass()
                val boxed = typeMapper.mapType(top.defaultType.makeNullable())
                StackValue.boxInlineClass(AsmTypes.OBJECT_TYPE, boxed, erasedSourceType.isNullable(), mv)
                return
            }

            val boxed = typeMapper.mapType(erasedSourceType, TypeMappingMode.CLASS_DECLARATION)
            StackValue.boxInlineClass(type, boxed, erasedSourceType.isNullable(), mv)

            if (typeMapper.mapType(erasedTargetType) == target) {
                if (!erasedSourceType.isSubtypeOf(erasedTargetType, codegen.context.typeSystem)) {
                    StackValue.coerce(boxed, target, mv, false)
                }
            }

            return
        }
        if (!isFromTypeUnboxed && isToTypeUnboxed) {
            val boxed = typeMapper.mapType(erasedTargetType, TypeMappingMode.CLASS_DECLARATION)
            val irClass = codegen.irFunction.parentAsClass
            if (irClass.isInline && irClass.symbol == irType.classifierOrNull && !irType.isNullable()) {
                // Use getfield instead of unbox-impl inside inline classes
                codegen.mv.getfield(boxed.internalName, irClass.inlineClassFieldName.asString(), target.descriptor)
            } else {
                if (irTarget.isInlineChildOfSealedInlineClass()) {
                    val top = irTarget.findTopSealedInlineSuperClass()
                    val boxedTop = typeMapper.mapType(top.defaultType.makeNullable())
                    StackValue.unboxInlineClass(type, boxedTop, AsmTypes.OBJECT_TYPE, erasedTargetType.isNullable(), mv)
                    StackValue.coerce(AsmTypes.OBJECT_TYPE, target, mv)
                    return
                }

                StackValue.unboxInlineClass(type, boxed, target, erasedTargetType.isNullable(), mv)
            }
            return
        }

        if ((type != target || (castForReified && irType.anyTypeArgument { it.isReified })) &&
            !(irTarget == irType && (irTarget.isInlineChildOfSealedInlineClass() || irType.isInlineChildOfSealedInlineClass()))
        ) {
            StackValue.coerce(type, target, mv, type == target)
        }
    }

    private fun mapUnboxedType(type: IrType): Type? =
        if (type.classOrNull?.owner?.isSealedInline == true) AsmTypes.OBJECT_TYPE
        else InlineClassAbi.unboxType(type)?.let(typeMapper::mapType)

    abstract fun discard()

    val mv: InstructionAdapter
        get() = codegen.mv

    val typeMapper: IrTypeMapper
        get() = codegen.typeMapper
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
