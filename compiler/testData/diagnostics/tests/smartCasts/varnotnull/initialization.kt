// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun foo() {
    var v: String? = "xyz"
    // It is possible in principle to provide smart cast here
    v<!UNSAFE_CALL!>.<!>length
    v = null
    v<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral */
