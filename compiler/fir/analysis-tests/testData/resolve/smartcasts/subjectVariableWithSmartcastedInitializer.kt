// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83903

sealed interface Foo {
    object Bar: Foo
    object Baz: Foo
}

fun test_1(foo: Foo?) {
    if (foo == null) return
    <!NO_ELSE_IN_WHEN!>when<!> (val it = foo) {
        is Foo.Bar -> "bar"
        is Foo.Baz -> "baz"
    }
}

fun test_2(foo: Foo?) {
    if (foo == null) return
    val it = foo
    when (it) {
        is Foo.Bar -> "bar"
        is Foo.Baz -> "baz"
    }
}

fun test_3(foo: Foo?) {
    if (foo == null) return
    when (foo) {
        is Foo.Bar -> "bar"
        is Foo.Baz -> "baz"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, sealed, smartcast, stringLiteral,
whenExpression, whenWithSubject */
