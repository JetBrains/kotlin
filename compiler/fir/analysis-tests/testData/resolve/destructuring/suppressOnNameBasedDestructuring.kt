// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring +EnableNameBasedDestructuringShortForm +LocalVariableTargetedAnnotationOnDestructuring

data class HasDeprecation(@Deprecated("") val x: String)

data class D(val x: String)

fun baz(foo: HasDeprecation) {
    @Suppress("DEPRECATION")
    val (x) = foo

    @Suppress("DEPRECATION")
    (val y = x) = foo

    @Suppress("UNCHECKED_CAST")
    val (z = x) = Any() as D

    @Suppress("UNCHECKED_CAST")
    (val z2 = x) = Any() as D

    @Suppress("UNCHECKED_CAST")
    val [_] = Any() as D

    @Suppress("UNCHECKED_CAST")
    [val _] = Any() as D
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, data, destructuringDeclaration,
functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
