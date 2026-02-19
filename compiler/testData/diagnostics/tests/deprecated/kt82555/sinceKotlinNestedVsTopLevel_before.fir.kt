// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// API_VERSION: 2.3

class C {
    @SinceKotlin("2.4")
    object Obj

    val obj = <!API_NOT_AVAILABLE!>Obj<!>
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration, propertyDeclaration, stringLiteral */
