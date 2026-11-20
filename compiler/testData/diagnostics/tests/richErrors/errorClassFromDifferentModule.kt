// RUN_PIPELINE_TILL: CODEGEN
// MODULE: m1
error class Foo
// MODULE: m2(m1)
fun foo(): String | Foo = ""

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral */
