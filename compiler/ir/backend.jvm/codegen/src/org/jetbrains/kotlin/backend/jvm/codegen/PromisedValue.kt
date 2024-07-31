/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.inlineClassFieldName
import org.jetbrains.kotlin.backend.jvm.ir.eraseIfTypeParameter
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOf
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
        val erasedSourceType = irType.eraseIfTypeParameter()
        val erasedTargetType = irTarget.eraseIfTypeParameter()
        val boxedSourceType = erasedSourceType.getBoxedTypeIfInlineClass() ?: type
        val boxedTargetType = erasedTargetType.getBoxedTypeIfInlineClass() ?: target
        if (type != boxedSourceType && target == boxedTargetType) {
            // `unboxed` can be different from `type` for generic inline classes, e.g. in `class IC<T>(val x: T)`
            // `box-impl` takes an `Object` but if this value is an `IC<String>` then `type` should be `String`.
            val unboxed = typeMapper.mapUnderlyingTypeOfInlineClassType(erasedSourceType)
            StackValue.boxInlineClass(type, unboxed, boxedSourceType, target, erasedSourceType.isNullable(), mv)
        } else if (type == boxedSourceType && target != boxedTargetType) {
            val unboxed = typeMapper.mapUnderlyingTypeOfInlineClassType(erasedTargetType)
            val irClass = codegen.irFunction.parentAsClass
            if (irClass.isSingleFieldValueClass && irClass.symbol == irType.classifierOrNull && !irType.isNullable()) {
                // Use GETFIELD instead of `unbox-impl` inside inline classes
                codegen.mv.getfield(boxedTargetType.internalName, irClass.inlineClassFieldName.asString(), unboxed.descriptor)
                StackValue.coerce(unboxed, target, mv, false)
            } else {
                StackValue.unboxInlineClass(type, boxedTargetType, unboxed, target, erasedTargetType.isNullable(), mv)
            }
        } else if (type != target || (castForReified && irType.anyTypeArgument { it.isReified })) {
            StackValue.coerce(type, target, mv, true)
        }
    }

    abstract fun discard()

    val mv: InstructionAdapter
        get() = codegen.mv

    val typeMapper: IrTypeMapper
        get() = codegen.typeMapper

    private fun IrType.getBoxedTypeIfInlineClass(): Type? =
        if (classOrNull?.owner?.inlineClassRepresentation != null)
            typeMapper.mapType(this, TypeMappingMode.CLASS_DECLARATION)
        else null
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

    fun markLineNumber(expression: IrExpression) {
        with(codegen) {
            expression.markLineNumber(startOffset = true)
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
