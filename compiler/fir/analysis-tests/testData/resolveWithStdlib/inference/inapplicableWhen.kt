// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82277

class Foo
enum class E { A, B }

fun test(foo: Foo, enum: E): Pair<Foo, Foo> {
    return <!WHEN_ON_SEALED!>when (enum) {
        E.A -> foo to foo
        E.B -> foo.<!UNRESOLVED_REFERENCE!>bar<!>() <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>to<!> foo
    }<!>
}

fun test2(foo: Foo, enum: E): Pair<Foo, Foo> {
    return (foo.<!UNRESOLVED_REFERENCE!>bar<!>() <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>to<!> foo)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast,
whenExpression, whenWithSubject */
