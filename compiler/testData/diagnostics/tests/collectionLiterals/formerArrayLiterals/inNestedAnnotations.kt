// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

annotation class Foo(val arr: Array<Bar> = [])
annotation class Bar(val arr: Array<Foo>)
annotation class Baz(val bar: Bar = Bar([]))
annotation class Bad(val bar: Bar = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>Bar([<!UNRESOLVED_REFERENCE!>[]<!>])<!>)

@Foo
@Bar([])
@Baz(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Bar([<!UNRESOLVED_REFERENCE!>[]<!>])<!>)
fun target() = Unit

@Foo([Bar([])])
@Bar([Foo([Bar([Foo()])])])
@Baz(Bar([Foo([Bar([])])]))
fun secondTarget() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, primaryConstructor,
propertyDeclaration */
