// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@file:Suppress("abc")

fun foo(): Int {
    @Suppress("xyz") return 1
}
