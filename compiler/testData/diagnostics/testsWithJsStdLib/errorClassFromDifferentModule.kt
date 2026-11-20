// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +RichErrors
// MODULE: m1
error class Foo
// MODULE: m2(m1)
fun foo(): String | Foo = ""
