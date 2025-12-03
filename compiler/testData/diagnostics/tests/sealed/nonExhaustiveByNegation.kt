// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-82841

sealed class Base

abstract class Derived : Base()

class Implementation : Derived()

fun test_1(x: Base) {
    val value = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        !is Implementation -> 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, localProperty,
propertyDeclaration, sealed, whenExpression, whenWithSubject */
