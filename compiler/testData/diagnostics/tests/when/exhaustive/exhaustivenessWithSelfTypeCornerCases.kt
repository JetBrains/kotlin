// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78097
// WITH_STDLIB
// LANGUAGE: +DataFlowBasedExhaustiveness

class A {
    companion object
}

fun foo(x: A): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        <!INCOMPATIBLE_TYPES!>A<!> -> 0
    }
}

open class B {
    companion object : B()
}

fun bar(x: B): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        B -> 0
    }
}

object X

fun baz(x: X): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        X -> 0
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, functionDeclaration, integerLiteral,
objectDeclaration, whenExpression, whenWithSubject */
