// FIR_IDENTICAL
//  ^ K1 is ignored
// LANGUAGE: +SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

package bar

class Outer {
    class A {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object

        fun foo() { }
    }

    fun test() {
        val ref = A::foo
        ref(bar.Outer.A())
        ref(<!ARGUMENT_TYPE_MISMATCH!>bar.A()<!>)
    }
}

class A {
    fun foo() { }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, localProperty,
nestedClass, objectDeclaration, propertyDeclaration, stringLiteral */
