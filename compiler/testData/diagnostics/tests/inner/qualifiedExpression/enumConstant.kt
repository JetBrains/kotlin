// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
enum class E {
    E1,
    E2 { };
}

fun foo() = E.E1
fun bar() = E.E2
