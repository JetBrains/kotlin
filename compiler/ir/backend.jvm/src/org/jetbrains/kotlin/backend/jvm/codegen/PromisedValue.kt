/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.eraseTypeParameters
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

// A value that may not have been fully constructed yet. The ability to "roll back" code generation
// is useful for certain optimizations.
abstract class PromisedValue(val codegen: ExpressionCodegen, val type: Type, val irType: IrType) {
    // If this value is immaterial, construct an object on the top of the stack. This
    // must always be done before generating other values or emitting raw bytecode.
    open fun materializeAt(target: Type, irTarget: IrType) {
        val erasedSourceType = irType.eraseTypeParameters()
        val erasedTargetType = irTarget.eraseTypeParameters()
        val isFromTypeInlineClass = erasedSourceType.classOrNull!!.owner.isInline
        val isToTypeInlineClass = erasedTargetType.classOrNull!!.owner.isInline

        // Boxing and unboxing kotlin.Result leads to CCE in generated code
        val doNotCoerceKotlinResultInContinuation =
            (codegen.irFunction.parentAsClass.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS ||
                    codegen.irFunction.parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA)
                    && (irType.isKotlinResult() || irTarget.isKotlinResult())

        // Coerce inline classes
        if ((isFromTypeInlineClass || isToTypeInlineClass) && !doNotCoerceKotlinResultInContinuation) {
            val isFromTypeUnboxed = isFromTypeInlineClass && typeMapper.mapType(erasedSourceType.unboxed) == type
            val isToTypeUnboxed = isToTypeInlineClass && typeMapper.mapType(erasedTargetType.unboxed) == target

            when {
                isFromTypeUnboxed && !isToTypeUnboxed -> {
                    StackValue.boxInlineClass(erasedSourceType.toKotlinType(), mv)
                    return
                }

                !isFromTypeUnboxed && isToTypeUnboxed -> {
                    StackValue.unboxInlineClass(type, erasedTargetType.toKotlinType(), mv)
                    return
                }
            }
        }

        if (type != target) {
            StackValue.coerce(type, target, mv)
        }
    }

    abstract fun discard()

    val mv: InstructionAdapter
        get() = codegen.mv

    val typeMapper: IrTypeMapper
        get() = codegen.typeMapper
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

    override fun materializeAt(target: Type, irTarget: IrType) {
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
    override fun materializeAt(target: Type, irTarget: IrType) {
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
                this@coerceToBoolean.materializeAt(Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType).also { mv.ifeq(target) }

            override fun jumpIfTrue(target: Label) =
                this@coerceToBoolean.materializeAt(Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType).also { mv.ifne(target) }

            override fun discard() {
                this@coerceToBoolean.discard()
            }
        }
    }


fun PromisedValue.materializedAt(target: Type, irTarget: IrType): MaterialValue {
    materializeAt(target, irTarget)
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

val IrType.unboxed: IrType
    get() = InlineClassAbi.getUnderlyingType(erasedUpperBound)

// A Non-materialized value of Unit type that is only materialized through coercion.
val ExpressionCodegen.unitValue: PromisedValue
    get() = MaterialValue(this, Type.VOID_TYPE, context.irBuiltIns.unitType)

val ExpressionCodegen.nullConstant: PromisedValue
    get() = object : PromisedValue(this, AsmTypes.OBJECT_TYPE, context.irBuiltIns.nothingNType) {
        override fun materializeAt(target: Type, irTarget: IrType) {
            mv.aconst(null)
        }

        override fun discard() {}
    }
