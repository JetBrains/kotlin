// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
object A {
    val a = 1
    val b = B.a
}

object B {
    val a = 2
}