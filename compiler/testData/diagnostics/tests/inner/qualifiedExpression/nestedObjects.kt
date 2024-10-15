// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
object A {
    object B {
        object C
    }
}

val a = A.B.C
