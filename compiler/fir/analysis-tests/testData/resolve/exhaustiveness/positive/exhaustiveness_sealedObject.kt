// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-37488

sealed class A

class B : A()
object C : A()

fun takeString(s: String) {}

fun test_1(a: A) {
    val s = <!WHEN_ON_SEALED_GEEN_ELSE!>when(a) {
        is B -> ""
        is C -> ""
    }<!>
    takeString(s)
}

fun test_2(a: A) {
    val s = <!WHEN_ON_SEALED_GEEN_ELSE!>when(a) {
        is B -> ""
        C -> ""
    }<!>
    takeString(s)
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, isExpression, localProperty,
objectDeclaration, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
