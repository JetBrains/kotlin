// LANGUAGE: +SkipHiddenObjectsInResolution
// FIR_IDENTICAL
//  ^ K1 is ignored
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82555

class Outer {
    class C {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object

        val obj = C
    }
}

object C

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, objectDeclaration, propertyDeclaration, stringLiteral */
