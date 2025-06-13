// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    var v: String? = null
    v<!UNSAFE_CALL!>.<!>length
    v = "abc"
    v.length
    v = null
    v<!UNSAFE_CALL!>.<!>length
    v = "abc"
    v.length
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral */
