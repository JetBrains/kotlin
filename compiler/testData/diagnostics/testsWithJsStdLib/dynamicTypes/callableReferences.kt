// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_EXPRESSION

fun test() {
    dynamic::foo
}

class dynamic {
    fun foo() {}
}