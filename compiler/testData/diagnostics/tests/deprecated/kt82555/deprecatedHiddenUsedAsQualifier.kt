// LANGUAGE_FEATURE_TOGGLED: SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82555

class C {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    object Obj

    val obj = Obj
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration, propertyDeclaration, stringLiteral */
