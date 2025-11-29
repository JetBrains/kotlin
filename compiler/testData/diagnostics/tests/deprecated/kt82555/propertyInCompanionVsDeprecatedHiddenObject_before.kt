// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND

class C {
    companion object {
        const val A: Int = 42
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    object A
}

annotation class Anno(val x: Int)

@Anno(C.A)
fun test() {
    C.A
    C.<!DEPRECATION_ERROR!>A<!>.<!UNRESOLVED_REFERENCE!>toLong<!>()
    C.A::toLong
    C.A::class
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, companionObject, const,
functionDeclaration, integerLiteral, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
