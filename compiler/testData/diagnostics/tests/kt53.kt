// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
val <T> T.foo : T?
    get() = null

fun test(): Int? {
    return 1.foo
}