// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_EXPRESSION

fun test() {
    dynamic::foo
}

class dynamic {
    fun foo() {}
}
