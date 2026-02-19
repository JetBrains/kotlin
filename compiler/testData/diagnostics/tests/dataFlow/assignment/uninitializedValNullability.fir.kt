// RUN_PIPELINE_TILL: FRONTEND
fun test(a: Any?, flag: Boolean, x: Any?) {
    if (a == null) return
    a.hashCode()

    val b: Any?

    if (flag) {
        b = a
        b.hashCode()
    }
    else {
        b = x
        b<!UNSAFE_CALL!>.<!>hashCode()
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast */
