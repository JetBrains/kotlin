// RUN_PIPELINE_TILL: BACKEND
class A {
    var v: String? = null
}

fun foo(): String? {
    val t = A().v

    return t
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, propertyDeclaration */
