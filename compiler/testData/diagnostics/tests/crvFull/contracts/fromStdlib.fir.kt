// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// Check that contract 'survives' across modules:
fun main(s: String?, sb: StringBuilder) {
    s?.let { sb.append(it) }
    s?.<!RETURN_VALUE_NOT_USED!>let<!> { sb.toString() + it }
}

fun foobar(s: String?, ss: String?) {
    val a: String
    s.let { a = s!! }
    a.<!RETURN_VALUE_NOT_USED!>length<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, checkNotNullCall, functionDeclaration, javaFunction,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
