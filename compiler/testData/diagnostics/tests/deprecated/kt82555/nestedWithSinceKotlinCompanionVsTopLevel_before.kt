// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// API_VERSION: 2.3

class C {
    class Obj {
        @SinceKotlin("2.4")
        companion object
    }

    val obj = Obj
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, propertyDeclaration,
stringLiteral */
