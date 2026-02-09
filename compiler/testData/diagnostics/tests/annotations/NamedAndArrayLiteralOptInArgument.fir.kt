// RUN_PIPELINE_TILL: BACKEND
@RequiresOptIn annotation class A
@RequiresOptIn annotation class B

@OptIn(markerClass = [A::class, B::class])
fun foo() {}

@OptIn(*[A::class, B::class])
fun foo2() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference, collectionLiteral, functionDeclaration */
