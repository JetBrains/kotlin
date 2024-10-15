// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
val zero = 0

fun test() {
    -0
    -0L
    -0.0
    -(1 - 1)
    -zero

    +0
}
