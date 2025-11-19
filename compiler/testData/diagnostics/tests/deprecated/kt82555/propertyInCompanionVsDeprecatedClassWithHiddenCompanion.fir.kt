// RUN_PIPELINE_TILL: FRONTEND

class C {
    companion object {
        const val A: Int = 42
    }

    class A {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object
    }
}

annotation class Anno(val x: Int)

@Anno(<!ARGUMENT_TYPE_MISMATCH!>C.<!DEPRECATION_ERROR!>A<!><!>)
fun test() {
    C.<!DEPRECATION_ERROR!>A<!>
    C.A::class

    // K2: both are unresolved without companion object (hence, should be unresolved with hidden companion)
    // K1: callable reference is resolved to property, call is unresolved
    C.A.<!UNRESOLVED_REFERENCE!>toLong<!>()
    C.A::<!UNRESOLVED_REFERENCE!>toLong<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, companionObject, const,
functionDeclaration, integerLiteral, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
