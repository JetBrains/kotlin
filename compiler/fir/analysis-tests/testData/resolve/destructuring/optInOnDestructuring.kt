// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring +EnableNameBasedDestructuringShortForm

@RequiresOptIn
annotation class A

class Foo(@property:A val bar: String)

fun test(foo: Foo) {
    <!WRONG_ANNOTATION_TARGET!>@OptIn(A::class)<!>
    val (<!OPT_IN_USAGE_ERROR!>bar<!>) = foo

    <!WRONG_ANNOTATION_TARGET!>@OptIn(A::class)<!>
    (val baz = <!OPT_IN_USAGE_ERROR!>bar<!>) = foo
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, destructuringDeclaration,
functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
