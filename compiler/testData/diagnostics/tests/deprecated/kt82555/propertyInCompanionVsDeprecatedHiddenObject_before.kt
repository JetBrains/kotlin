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

@Anno(<!ARGUMENT_TYPE_MISMATCH!>C.<!DEPRECATION_ERROR!>A<!><!>)
fun test() {
    C.<!DEPRECATION_ERROR!>A<!>
    C.<!DEPRECATION_ERROR!>A<!>.<!UNRESOLVED_REFERENCE!>toLong<!>()
    C.<!DEPRECATION_ERROR!>A<!>::<!UNRESOLVED_REFERENCE!>toLong<!>
    C.<!DEPRECATION_ERROR!>A<!>::class
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, companionObject, const,
functionDeclaration, integerLiteral, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
