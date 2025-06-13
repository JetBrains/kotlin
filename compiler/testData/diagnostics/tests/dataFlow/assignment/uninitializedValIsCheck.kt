// RUN_PIPELINE_TILL: FRONTEND
fun test(a: Any?, flag: Boolean, x: Any?) {
    if (a !is String) return
    <!DEBUG_INFO_SMARTCAST!>a<!>.length

    val b: Any?

    if (flag) {
        b = a
        <!DEBUG_INFO_SMARTCAST!>b<!>.length
    }
    else {
        b = x
        b.<!UNRESOLVED_REFERENCE!>length<!>()
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, ifExpression, isExpression, localProperty, nullableType,
propertyDeclaration, smartcast */
