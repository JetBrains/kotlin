// RUN_PIPELINE_TILL: BACKEND
inline fun <reified T : Any> foo(t: T): T {
    val klass = T::class.java
    return t
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, inline, localProperty, propertyDeclaration, reified,
typeConstraint, typeParameter */
