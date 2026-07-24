// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82555
// LANGUAGE: +NestedTypeAliases
// LANGUAGE_FEATURE_TOGGLED: SkipHiddenObjectsInResolution

class Outer {
    class C {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object
    }

    typealias Obj = C

    val obj = Obj
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, propertyDeclaration,
stringLiteral, typeAliasDeclaration */
