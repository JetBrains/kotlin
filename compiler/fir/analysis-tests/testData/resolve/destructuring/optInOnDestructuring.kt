// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring +EnableNameBasedDestructuringShortForm +LocalVariableTargetedAnnotationOnDestructuring

@RequiresOptIn
annotation class A

class Foo(@property:A val bar: String)

fun test(foo: Foo) {
    @OptIn(A::class)
    val (<!OPT_IN_USAGE_ERROR!>bar<!>) = foo

    @OptIn(A::class)
    (val baz = <!OPT_IN_USAGE_ERROR!>bar<!>) = foo
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, destructuringDeclaration,
functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
