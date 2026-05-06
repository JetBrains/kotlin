// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-2880
// WITH_STDLIB

// KT-2880: Confusing error message when trying to modify non-mutable map
fun main() {
    val map: Map<String, Boolean> = HashMap()
    map<!NO_SET_METHOD!>["a"]<!> = true
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, localProperty, propertyDeclaration, stringLiteral */
