// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

sealed class A
sealed class B : A()
sealed class C : B()
object BB : B()
object CC : C()

fun foo(a: A) {
    if (a is B) {
        if (a is C) {
            val t = <!WHEN_ON_SEALED!>when (a) {
                is CC -> "CC"
            }<!>
        }
    }
}

fun foo2(a: A) {
    if (a is C) {
        if (<!USELESS_IS_CHECK!>a is B<!>) {
            val t = <!WHEN_ON_SEALED!>when (a) {
                is CC -> "CC"
            }<!>
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
objectDeclaration, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
