// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring +EnableNameBasedDestructuringShortForm +LocalVariableTargetedAnnotationOnDestructuring

@RequiresOptIn
annotation class A

class Foo(@property:A val bar: String)

fun test(foo: Foo) {
    @OptIn(A::class)
    val (bar) = foo

    @OptIn(A::class)
    (val baz = bar) = foo
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, destructuringDeclaration,
functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
