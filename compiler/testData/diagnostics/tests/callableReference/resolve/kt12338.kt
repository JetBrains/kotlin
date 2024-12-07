// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// KT-12338 Compiler error ERROR: Rewrite at slice LEXICAL_SCOPE key: REFERENCE_EXPRESSION with when and function references

fun a() { }

fun test() {
    when {
        true -> ::a
    }
}
