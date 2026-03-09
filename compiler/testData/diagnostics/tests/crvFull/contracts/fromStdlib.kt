// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// Check that contract 'survives' across modules:
fun main(s: String?, sb: StringBuilder) {
    s?.let { sb.append(it) }
    s?.let { sb.toString() + it }
}

fun foobar(s: String?, ss: String?) {
    val a: String
    s.let { a = s!! }
    a.length
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, checkNotNullCall, functionDeclaration, javaFunction,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
