// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// API_VERSION: 2.3

class C {
    @SinceKotlin("2.4")
    object Obj

    val obj = Obj
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration, propertyDeclaration, stringLiteral */
