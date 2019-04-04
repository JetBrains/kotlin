/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

// A value that may not have been fully constructed yet. The ability to "roll back" code generation
// is useful for certain optimizations.
abstract class PromisedValue(val codegen: ExpressionCodegen, val type: Type) {
    // If this value is immaterial, construct an object on the top of the stack. This
    // must always be done before generating other values or emitting raw bytecode.
    abstract fun materialize()
}

// A value that *has* been fully constructed.
class MaterialValue(codegen: ExpressionCodegen, type: Type) : PromisedValue(codegen, type) {
    override fun materialize() {}
}

// A value that can be branched on. JVM has certain branching instructions which can be used
// to optimize these.
abstract class BooleanValue(codegen: ExpressionCodegen) : PromisedValue(codegen, Type.BOOLEAN_TYPE) {
    abstract fun jumpIfFalse(target: Label)
    abstract fun jumpIfTrue(target: Label)

    override fun materialize() {
        val const0 = Label()
        val end = Label()
        jumpIfFalse(const0)
        codegen.mv.iconst(1)
        codegen.mv.goTo(end)
        codegen.mv.mark(const0)
        codegen.mv.iconst(0)
        codegen.mv.mark(end)
    }
}

// Same as materialize(), but return a representation of the result.
val PromisedValue.materialized: MaterialValue
    get() {
        materialize()
        return MaterialValue(codegen, type)
    }

// Materialize and disregard this value. Materialization is forced because, presumably,
// we only wanted the side effects anyway.
fun PromisedValue.discard(): MaterialValue {
    materialize()
    if (type !== Type.VOID_TYPE)
        AsmUtil.pop(codegen.mv, type)
    return MaterialValue(codegen, Type.VOID_TYPE)
}

// On materialization, cast the value to a different type.
fun PromisedValue.coerce(target: Type) = when (target) {
    type -> this
    else -> object : PromisedValue(codegen, target) {
        // TODO remove dependency
        override fun materialize() = StackValue.coerce(this@coerce.materialized.type, type, codegen.mv)
    }
}

// Same as above, but with a return type that allows conditional jumping.
fun PromisedValue.coerceToBoolean() = when (val coerced = coerce(Type.BOOLEAN_TYPE)) {
    is BooleanValue -> coerced
    else -> object : BooleanValue(codegen) {
        override fun jumpIfFalse(target: Label) = coerced.materialize().also { codegen.mv.ifeq(target) }
        override fun jumpIfTrue(target: Label) = coerced.materialize().also { codegen.mv.ifne(target) }
        override fun materialize() = coerced.materialize()
    }
}

val ExpressionCodegen.voidValue: MaterialValue
    get() = MaterialValue(this, Type.VOID_TYPE)
