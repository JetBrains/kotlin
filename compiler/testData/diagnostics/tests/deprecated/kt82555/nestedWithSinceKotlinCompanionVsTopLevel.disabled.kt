// LANGUAGE_FEATURE_TOGGLED: SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: BACKEND
// API_VERSION: 2.3

class C {
    class Obj {
        @SinceKotlin("2.4")
        companion object
    }

    val obj = <!API_NOT_AVAILABLE!>Obj<!>
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, propertyDeclaration,
stringLiteral */
