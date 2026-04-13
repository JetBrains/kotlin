// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-30045
// WITH_STDLIB

// KT-30045: False positive USELESS_CAST on `dynamic` value used with spread operator
fun foo() {
    val data: dynamic = ""
    val list: List<String> = when (data) {
        is String -> listOf(data)
        is Array<String> -> listOf(*data as Array<String>)
        else -> TODO()
    }
}

/* GENERATED_FIR_TAGS: asExpression, flexibleType, functionDeclaration, isExpression, localProperty, propertyDeclaration,
smartcast, stringLiteral, whenExpression, whenWithSubject */
