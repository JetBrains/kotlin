// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND

fun test(l: List<String>) {
    for (<!WRONG_MODIFIER_TARGET!>lateinit<!> x in l) {}
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, localProperty, propertyDeclaration */
