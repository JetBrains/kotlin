// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

sealed class A
sealed class B : A()
sealed class C : A()
object BB : B()
object CC : C()

fun foo(a: A) {
    if (a is B) {
        if (<!IMPOSSIBLE_IS_CHECK_ERROR!>a is C<!>) {
            val t = when (a) {
                <!IMPOSSIBLE_IS_CHECK_ERROR!>is CC<!> -> "CC"
            }
        }
    }
}

fun foo2(a: A) {
    if (a is C) {
        if (<!IMPOSSIBLE_IS_CHECK_ERROR!>a is B<!>) {
            val t = when (a) {
                    <!IMPOSSIBLE_IS_CHECK_ERROR!>is CC<!> -> "CC"
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, intersectionType, isExpression,
localProperty, objectDeclaration, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
