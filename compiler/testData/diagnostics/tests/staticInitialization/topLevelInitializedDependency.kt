// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
val a = 5
val b = A.b

object A {
    val b = a
}