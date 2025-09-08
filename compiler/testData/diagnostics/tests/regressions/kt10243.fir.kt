// RUN_PIPELINE_TILL: FRONTEND
val f: Boolean = true
private fun doUpdateRegularTasks() {
    try {
        while (f) {
            val xmlText = <!UNRESOLVED_REFERENCE!>getText<!>()
            if (xmlText == null) {}
            else {
                xmlText.<!UNRESOLVED_REFERENCE!>value<!> = 0 // !!!
            }
        }

    }
    finally {
        fun execute() {}
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, integerLiteral, localFunction,
localProperty, propertyDeclaration, smartcast, tryExpression, whileLoop */
