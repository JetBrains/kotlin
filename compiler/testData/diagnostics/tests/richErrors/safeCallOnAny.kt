// RUN_PIPELINE_TILL: CODEGEN
fun test(a: Any) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String | kotlin.RichError")!>a|.toString()<!>
    a|.foo()
}

fun NonError.foo() {}

/* GENERATED_FIR_TAGS: functionDeclaration, safeCall */
