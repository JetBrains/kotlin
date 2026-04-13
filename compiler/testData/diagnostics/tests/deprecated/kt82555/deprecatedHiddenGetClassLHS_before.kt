// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82555

class Outer {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    class C {
        fun foo() { }
    }

    val ref = C::class
}

class C

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nestedClass, propertyDeclaration,
stringLiteral */
