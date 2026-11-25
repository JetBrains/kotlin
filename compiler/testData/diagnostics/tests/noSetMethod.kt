// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-92044
// RENDER_DIAGNOSTICS_FULL_TEXT

fun main() {
    val map: Map<String, Boolean> = HashMap()
    map<!NO_SET_METHOD!>["a"]<!> = true
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, localProperty, propertyDeclaration, stringLiteral */
