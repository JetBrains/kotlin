// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

class Outer {
    class A {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object

        object B {
            fun bar() { }
        }
    }

    fun test() {
        A.B.bar()
        A.B.<!UNRESOLVED_REFERENCE!>foo<!>()

        val ab = A.B
        ab.bar()
        ab.<!UNRESOLVED_REFERENCE!>foo<!>()

        val a = <!NO_COMPANION_OBJECT!>A<!>
    }
}

class A {
    object B {
        fun foo() { }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nestedClass,
objectDeclaration, propertyDeclaration, stringLiteral */
