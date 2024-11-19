// FIR_IDENTICAL
// ISSUE: KT-73153
// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt

private class A

// FILE: b.kt

class B<T>

fun <R> foo(): R = null!!

fun test() {
    B<<!INVISIBLE_REFERENCE!>A<!>>()
    foo<<!INVISIBLE_REFERENCE!>A<!>>()
}
