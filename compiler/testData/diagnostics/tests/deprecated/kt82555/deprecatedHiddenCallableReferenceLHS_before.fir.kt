// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

class Outer {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    class C {
        fun foo() { }
    }

    val ref = C::toString
    val wrongRef = C::<!UNRESOLVED_REFERENCE!>foo<!>
}

class C

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nestedClass, propertyDeclaration,
stringLiteral */
