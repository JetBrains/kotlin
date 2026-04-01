// LANGUAGE: +SkipHiddenObjectsInResolution
// FIR_IDENTICAL
//  ^ K1 is ignored
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82555
// FIR_DUMP

class Outer {
    class A {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object
    }

    fun test() {
        val ref = A::class
    }
}

class A

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, functionDeclaration, localProperty,
nestedClass, objectDeclaration, propertyDeclaration, stringLiteral */
