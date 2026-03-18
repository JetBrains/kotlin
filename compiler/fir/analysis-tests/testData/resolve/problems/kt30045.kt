// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-30045
// WITH_STDLIB

// KT-30045: False positive USELESS_CAST on `dynamic` value used with spread operator
fun foo() {
    val data: <!UNSUPPORTED!>dynamic<!> = ""
    val list: List<String> = when (data) {
        is String -> <!UNSUPPORTED!>listOf<!>(data)
        is Array<String> -> listOf(*data as Array<String>)
        else -> TODO()
    }
}

/* GENERATED_FIR_TAGS: asExpression, flexibleType, functionDeclaration, isExpression, localProperty, propertyDeclaration,
smartcast, stringLiteral, whenExpression, whenWithSubject */
