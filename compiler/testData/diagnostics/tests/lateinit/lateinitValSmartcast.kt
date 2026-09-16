// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

lateinit val x: Any

fun test() {
    if (x is String) {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, isExpression, lateinit, propertyDeclaration, smartcast */
