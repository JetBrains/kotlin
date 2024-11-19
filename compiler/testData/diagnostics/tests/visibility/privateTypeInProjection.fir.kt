// ISSUE: KT-73153
// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt

private class A

// FILE: b.kt

class B<T>

fun <R> foo(): R = null!!

fun test() {
    B<A>()
    foo<A>()
}
