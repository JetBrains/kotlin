// RUN_PIPELINE_TILL: FRONTEND
fun bar(): <!UNSUPPORTED!>dynamic<!> = TODO()

fun foo() {
    val x = bar()
    if (x is String) {
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("dynamic")!>x<!>
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, ifExpression, isExpression, localProperty, propertyDeclaration,
smartcast */
