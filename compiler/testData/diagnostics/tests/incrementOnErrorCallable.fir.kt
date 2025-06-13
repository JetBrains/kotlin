// RUN_PIPELINE_TILL: FRONTEND
val a: Int
    get() = 10

fun test() {
    a.<!UNRESOLVED_REFERENCE!>shrek<!>.<!UNRESOLVED_REFERENCE!>brek<!> += 10
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, getter, integerLiteral, localProperty,
propertyDeclaration */
