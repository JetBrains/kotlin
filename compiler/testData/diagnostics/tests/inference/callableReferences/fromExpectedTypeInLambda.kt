// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-73011

fun <X> id(x: X): X = x

fun foo(x: Any?) {}
fun foo() {}

fun test0(): (Any?) -> Unit =
    id(::foo) // OK

fun test1(): (Any?) -> Unit =
    run {
        ::foo // OK
    }

fun test2(): (Any?) -> Unit =
    id(run {
        ::foo // UNRESOLVED_REFERENCE
    })
