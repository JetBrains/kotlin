// RUN_PIPELINE_TILL: BACKEND
fun test(loop: Boolean) {
    while (loop) {
        try {
            do {
                run<Unit> {
                    val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>a<!>: String
                    if (loop) {
                        <!UNUSED_VALUE!>a =<!> ""
                    } else {
                        <!UNUSED_VALUE!>a =<!> ""
                    }
                }
            } while (loop)
        } catch (e: Exception) {
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, doWhileLoop, functionDeclaration, ifExpression, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, tryExpression, whileLoop */
