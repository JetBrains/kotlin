// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-40674

fun main() {
    genericLet("", ::foo) // Should be Ok
    genericLet<String>("", ::foo) // Should be Ok

    nonGeneric(::foo) // OK
}

fun <T> genericLet(t: T, b: (T) -> Unit) {}

fun nonGeneric(x: (String) -> Unit) {}

fun <T> foo(vararg elements: T) {}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, nullableType, stringLiteral,
typeParameter, vararg */
