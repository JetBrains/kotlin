// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

sealed class A
sealed class B : A()
sealed class C : A()
object BB : B()
object CC : C()

fun foo(a: A) {
    if (a is B) {
        if (<!USELESS_IS_CHECK!>a is C<!>) {
            val t = when (a) {
                <!USELESS_IS_CHECK!>is CC<!> -> "CC"
            }
        }
    }
}

fun foo2(a: A) {
    if (a is C) {
        if (<!USELESS_IS_CHECK!>a is B<!>) {
            val t = when (a) {
                    <!USELESS_IS_CHECK!>is CC<!> -> "CC"
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, intersectionType, isExpression,
localProperty, objectDeclaration, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
