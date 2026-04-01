// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-76739

interface Foo<T>

fun <T> foo(s: String) {}
fun test() {
    foo<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<!>>("")
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, stringLiteral, typeParameter */
