// RUN_PIPELINE_TILL: BACKEND
data class My(val x: Unit)

fun foo(my: My?): Int? {
    val x = my?.x
    // ?. is required here
    return x?.hashCode()
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, localProperty, nullableType, primaryConstructor,
propertyDeclaration, safeCall */
