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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

// A value that may not have been fully constructed yet. The ability to "roll back" code generation
// is useful for certain optimizations.
abstract class PromisedValue(val codegen: ExpressionCodegen, val type: Type, val irType: IrType) {
    // If this value is immaterial, construct an object on the top of the stack. This
    // must always be done before generating other values or emitting raw bytecode.
    abstract fun materialize()

    val mv: InstructionAdapter
        get() = codegen.mv

    val typeMapper: IrTypeMapper
        get() = codegen.typeMapper

    val kotlinType: KotlinType
        get() = irType.toKotlinType()
}

// A value that *has* been fully constructed.
class MaterialValue(codegen: ExpressionCodegen, type: Type, irType: IrType) : PromisedValue(codegen, type, irType) {
    override fun materialize() {}
}

// A value that is only materialized through coercion.
class ImmaterialValue(codegen: ExpressionCodegen, type: Type, irType: IrType) : PromisedValue(codegen, type, irType) {
    override fun materialize() {}
}

// A value that can be branched on. JVM has certain branching instructions which can be used
// to optimize these.
abstract class BooleanValue(codegen: ExpressionCodegen) : PromisedValue(codegen, Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType) {
    abstract fun jumpIfFalse(target: Label)
    abstract fun jumpIfTrue(target: Label)

    override fun materialize() {
        val const0 = Label()
        val end = Label()
        jumpIfFalse(const0)
        mv.iconst(1)
        mv.goTo(end)
        mv.mark(const0)
        mv.iconst(0)
        mv.mark(end)
    }
}

// Same as materialize(), but return a representation of the result.
val PromisedValue.materialized: MaterialValue
    get() {
        materialize()
        return MaterialValue(codegen, type, irType)
    }

// Materialize and disregard this value. Materialization is forced because, presumably,
// we only wanted the side effects anyway.
fun PromisedValue.discard(): PromisedValue {
    materialize()
    if (type !== Type.VOID_TYPE)
        AsmUtil.pop(mv, type)
    return codegen.immaterialUnitValue
}

private val IrType.unboxed: IrType
    get() = InlineClassAbi.getUnderlyingType(erasedUpperBound)

// On materialization, cast the value to a different type.
fun PromisedValue.coerce(target: Type, irTarget: IrType): PromisedValue {
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
            isFromTypeUnboxed && !isToTypeUnboxed -> return object : PromisedValue(codegen, target, irTarget) {
                override fun materialize() {
                    this@coerce.materialize()
                    StackValue.boxInlineClass(erasedSourceType.toKotlinType(), mv)
                }
            }
            !isFromTypeUnboxed && isToTypeUnboxed -> return object : PromisedValue(codegen, target, irTarget) {
                override fun materialize() {
                    val value = this@coerce.materialized
                    StackValue.unboxInlineClass(value.type, erasedTargetType.toKotlinType(), mv)
                }
            }
        }
    }

    return when {
        // All unsafe coercions between irTypes should use the UnsafeCoerce intrinsic
        type == target -> this
        type == AsmTypes.JAVA_CLASS_TYPE && target == AsmTypes.K_CLASS_TYPE && irType.isKClass() ->
            object : PromisedValue(codegen, target, irTarget) {
                override fun materialize() {
                    this@coerce.materialize()
                    AsmUtil.wrapJavaClassIntoKClass(mv)
                }
            }
        type == AsmTypes.JAVA_CLASS_ARRAY_TYPE && target == AsmTypes.K_CLASS_ARRAY_TYPE && irType.isArrayOfKClass() ->
            object : PromisedValue(codegen, target, irTarget) {
                override fun materialize() {
                    this@coerce.materialize()
                    AsmUtil.wrapJavaClassesIntoKClasses(mv)
                }
            }
        else -> object : PromisedValue(codegen, target, irTarget) {
            override fun materialize() {
                val value = this@coerce.materialized
                StackValue.coerce(value.type, type, mv)
            }
        }
    }
}

fun PromisedValue.coerce(irTarget: IrType) =
    coerce(typeMapper.mapType(irTarget), irTarget)

fun PromisedValue.coerceToBoxed(irTarget: IrType) =
    coerce(typeMapper.boxType(irTarget), irTarget)

fun PromisedValue.boxInlineClasses(irTarget: IrType) =
    if (irTarget.classOrNull?.owner?.isInline == true)
        coerceToBoxed(irTarget) else this

// Same as above, but with a return type that allows conditional jumping.
fun PromisedValue.coerceToBoolean() =
    when (val coerced = coerce(Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType)) {
        is BooleanValue -> coerced
        else -> object : BooleanValue(codegen) {
            override fun jumpIfFalse(target: Label) = coerced.materialize().also { mv.ifeq(target) }
            override fun jumpIfTrue(target: Label) = coerced.materialize().also { mv.ifne(target) }
            override fun materialize() = coerced.materialize()
        }
    }

// Non-materialized value of Unit type
// This value is only materialized when calling coerce, which is why it is represented
// as a MaterialValue even though there is nothing on the stack.
val ExpressionCodegen.immaterialUnitValue: ImmaterialValue
    get() = ImmaterialValue(this, Type.VOID_TYPE, context.irBuiltIns.unitType)

// Non-materialized default value for the given type
fun ExpressionCodegen.defaultValue(irType: IrType): PromisedValue =
    object : PromisedValue(this, typeMapper.mapType(irType), irType) {
        override fun materialize() {
            StackValue.coerce(Type.VOID_TYPE, type, codegen.mv)
        }
    }

private fun IrType.isArrayOfKClass(): Boolean =
    isArray() && this is IrSimpleType && arguments.singleOrNull().let { argument ->
        argument is IrTypeProjection && argument.type.isKClass()
    }
