// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

class Outer {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    class C {
        fun foo() { }
    }

    val ref = <!DEPRECATION_ERROR!>C<!>::toString
    val wrongRef = <!DEPRECATION_ERROR!>C<!>::foo
}

class C

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nestedClass, propertyDeclaration,
stringLiteral */
