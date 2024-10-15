// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    fun foo() {}
}

typealias CA = C

val cf = CA::foo
