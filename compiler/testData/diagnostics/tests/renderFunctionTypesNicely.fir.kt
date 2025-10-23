// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74461
// RENDER_DIAGNOSTICS_MESSAGES
// LANGUAGE: +ContextParameters

fun main() {
    val a: (() -> Int) -> String <!INITIALIZER_TYPE_MISMATCH("(() -> Int) -> String; Int")!>=<!> 10
    val b: Int.(String, Boolean) -> String <!INITIALIZER_TYPE_MISMATCH("Int.(String, Boolean) -> String; Int")!>=<!> 10
    val c: context(Int) Int.(String) -> String <!INITIALIZER_TYPE_MISMATCH("context(Int) Int.(String) -> String; Int")!>=<!> 10
    val d: suspend Int.(String) -> String <!INITIALIZER_TYPE_MISMATCH("suspend Int.(String) -> String; Int")!>=<!> 10
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, localProperty, propertyDeclaration */
