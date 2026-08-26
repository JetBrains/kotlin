// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87788

fun test(): Int {
    var a: Any = 4
	val block: () -> Unit = { a = "test" }

    if (a !is Number) return 1
   	block()

    return when (a) { // Must be implicit
        is String -> 2
        else -> 3
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, ifExpression, integerLiteral, isExpression,
lambdaLiteral, localProperty, propertyDeclaration, stringLiteral, whenExpression, whenWithSubject */
