// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-49035
@file:Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)

// KT-49035: @kotlin.internal.Exact type inference error message has reversed expected/actual types
fun <T> foo(it: @kotlin.internal.Exact T) {}
fun main() {
    foo<Any>(<!ARGUMENT_TYPE_MISMATCH!>""<!>) // Should say: inferred type is String but Any was expected
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, functionDeclaration, nullableType, stringLiteral, typeParameter */
