// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

annotation class Foo(val arr: Array<Bar> = [])
annotation class Bar(val arr: Array<Foo>)
annotation class Baz(val bar: Bar = Bar([]))
annotation class Bad(val bar: Bar = Bar(<!ARGUMENT_TYPE_MISMATCH!>[[]]<!>))

@Foo
@Bar([])
@Baz(Bar(<!ARGUMENT_TYPE_MISMATCH!>[[]]<!>))
fun target() = Unit

@Foo([Bar([])])
@Bar([Foo([Bar([Foo()])])])
@Baz(Bar([Foo([Bar([])])]))
fun secondTarget() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, primaryConstructor,
propertyDeclaration */
