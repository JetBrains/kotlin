// RUN_PIPELINE_TILL: FRONTEND
const val c = <!UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH!>1u<!> + <!UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH!>2u<!>

fun box() = when {
    c != <!UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH!>3u<!> -> "fail"
    else -> "OK"
}

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, functionDeclaration, propertyDeclaration,
stringLiteral, unsignedLiteral, whenExpression */
