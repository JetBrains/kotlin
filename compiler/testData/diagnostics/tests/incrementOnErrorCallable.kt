// RUN_PIPELINE_TILL: FRONTEND
val a: Int
    get() = 10

fun test() {
    a.<!UNRESOLVED_REFERENCE!>shrek<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>brek<!> <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, DEBUG_INFO_MISSING_UNRESOLVED!>+=<!> 10
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, getter, integerLiteral, localProperty,
propertyDeclaration */
