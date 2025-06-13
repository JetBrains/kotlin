// RUN_PIPELINE_TILL: FRONTEND
fun test(a: Any?) {
    if (a == null) return
    a.hashCode()

    val b = a
    b.hashCode()

    val c: Any? = a
    c<!UNSAFE_CALL!>.<!>hashCode()
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast */
