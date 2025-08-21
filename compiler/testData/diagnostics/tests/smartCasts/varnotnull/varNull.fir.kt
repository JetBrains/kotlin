// RUN_PIPELINE_TILL: FRONTEND
fun foo(): Int {
    var s: String? = "abc"
    s = null
    return s<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral */
