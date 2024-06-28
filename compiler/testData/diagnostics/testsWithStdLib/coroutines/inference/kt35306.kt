// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.experimental.ExperimentalTypeInference

interface Build<T>

@OptIn(ExperimentalTypeInference::class)
fun <T> build(fn: Builder<T>.() -> Unit): Build<T> = TODO()

// Works completely
val build = build {
    value(1)
}

// Works completely
val buildWithWrappedValue = build {
    wrappedValue(Wrapped(1))
}

// Works completely
val buildWithFn = build {
    valueFn {
        1
    }
}

// Works, but the ide complains with "Non-applicable call for builder inference"
val buildWithFnWrapped = build {
    wrappedValueFn {
        Wrapped(1)
    }
}

interface Builder<T> {
    fun value(value: T)
    fun wrappedValue(value: Wrapped<T>)
    fun wrappedValueFn(fn: () -> Wrapped<T>)
    fun valueFn(fn: () -> T)
}

data class Wrapped<T>(val value: T)