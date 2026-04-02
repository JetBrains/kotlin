// RUN_PIPELINE_TILL: FRONTEND
fun intBinEq() {
    var x = 0
    x <!NONE_APPLICABLE!>+=<!> 'a'
    x += 1.toByte()
    x += 1.toShort()
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1L
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1f
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1.0
    x <!NONE_APPLICABLE!>*=<!> 'a'
    x *= 1.toByte()
    x *= 1.toShort()
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1L
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1f
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1.0
}

fun shortBinEq() {
    var x = 0.toShort()
    x <!NONE_APPLICABLE!>+=<!> 'a'
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1.toByte()
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1.toShort()
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1L
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1f
    x <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> 1.0

    x <!NONE_APPLICABLE!>*=<!> 'a'
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1.toByte()
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1.toShort()
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1L
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1f
    x <!ASSIGNMENT_TYPE_MISMATCH!>*=<!> 1.0
}

class A {
    operator fun plus(x : A) : A { return x }
}

class B {
    operator fun plus(x : A) : A { return x }
}

fun overloading() {
    var x = A()
    var y = A()
    x += y
    var z = B()
    z <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> x
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, functionDeclaration, integerLiteral,
intersectionType, localProperty, multiplicativeExpression, operator, propertyDeclaration, smartcast */
