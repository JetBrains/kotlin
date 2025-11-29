// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

class Outer {
    class C {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object

        val obj = <!DEPRECATION_ERROR!>C<!>
    }
}

object C

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, objectDeclaration, propertyDeclaration, stringLiteral */
