// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty1

class Inv<T>

fun <T, R : Number> foo(prop: KProperty1<T, R>, p: String = "") {}

fun <T, R> foo(prop: KProperty1<T, Inv<R>>, p: Int = 42) {}

class A {
    val prop = 42
}

fun test() {
    foo(A::prop)
}