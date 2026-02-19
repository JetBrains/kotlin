// RUN_PIPELINE_TILL: FRONTEND
fun main(args: Array<String>) {
    val x = 42
    when (x) {is<!SYNTAX!><!>}
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, isExpression, localProperty, propertyDeclaration,
whenExpression, whenWithSubject */
