// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

inline class IC(val value: Any)

fun <T> foo(a: Result<T>, ic: IC): Pair<T, Any> = bar(a, ic) { a, ic ->
    a.getOrThrow() to ic.value
}

fun interface FunIFace<T1, T2, R> {
    fun call(t1: T1, t2: T2): R
}

fun <T1, T2, R> bar(t1: T1, t2: T2, f: FunIFace<T1, T2, R>): R {
    return f.call(t1, t2)
}

fun Pair<Any, Any>.join(): String = "$first$second"

fun box(): String = foo<Any>(Result.success("O"), IC("K")).join()