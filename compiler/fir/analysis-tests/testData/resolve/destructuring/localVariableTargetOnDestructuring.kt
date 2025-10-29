// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring -LocalVariableTargetedAnnotationOnDestructuring

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Ann

data class Foo(val x: String)

fun bar(foo: Foo) {
    <!WRONG_ANNOTATION_TARGET!>@Ann<!> val (x) = foo
    <!WRONG_ANNOTATION_TARGET!>@Ann<!> val [b] = foo
    <!WRONG_ANNOTATION_TARGET!>@Ann<!> [val c] = foo
    <!WRONG_ANNOTATION_TARGET!>@Ann<!> (val y = x) = foo
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, data, destructuringDeclaration,
functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
