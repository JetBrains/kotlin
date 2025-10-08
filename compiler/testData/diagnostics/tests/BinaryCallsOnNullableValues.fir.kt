// RUN_PIPELINE_TILL: FRONTEND
class A() {
    override fun equals(other : Any?) : Boolean = false
}

fun f(): Unit {
    var x: Int? = 1
    x = null
    x + 1
    x <!INFIX_MODIFIER_REQUIRED!>plus<!> 1
    x <!UNSAFE_OPERATOR_CALL!><<!> 1
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1

    x == 1
    x != 1

    <!EQUALITY_NOT_APPLICABLE!>A() == 1<!>

    <!FORBIDDEN_IDENTITY_EQUALS!>x === "1"<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>x !== "1"<!>

    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>x === 1<!>
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>x !== 1<!>

    x..2
    x in 1..2

    val y : Boolean? = true
    false || <!CONDITION_TYPE_MISMATCH!>y<!>
    <!CONDITION_TYPE_MISMATCH!>y<!> && true
    <!CONDITION_TYPE_MISMATCH!>y<!> && <!CONDITION_TYPE_MISMATCH!>1<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, assignment, classDeclaration, comparisonExpression,
disjunctionExpression, equalityExpression, functionDeclaration, integerLiteral, intersectionType, localProperty,
nullableType, operator, override, primaryConstructor, propertyDeclaration, rangeExpression, smartcast, stringLiteral */
