// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring +LocalVariableTargetedAnnotationOnDestructuring

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Ann

data class Foo(val x: String)

fun bar(foo: Foo) {
    @Ann val (x) = foo
    @Ann val [b] = foo
    @Ann [val c] = foo
    @Ann (val y = x) = foo
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, data, destructuringDeclaration,
functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
