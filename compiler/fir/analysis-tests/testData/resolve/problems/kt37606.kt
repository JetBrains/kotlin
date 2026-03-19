// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-37606
// WITH_STDLIB

// KT-37606: Confusing error message due to missing candidates when trying to mutate read-only Map
private val cache: Map<String, String>
    get() = TODO()

fun foo() {
    cache<!NO_SET_METHOD!>["foo"]<!> = "bar"
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, getter, propertyDeclaration, stringLiteral */
