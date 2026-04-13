// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

class A {
    class B {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object

        class C {
            object D
        }
    }
}

typealias AB = A.B

fun test() {
    <!DEPRECATION_ERROR!>AB<!>
    A.<!DEPRECATION_ERROR!>B<!>
    A.B.<!NO_COMPANION_OBJECT!>C<!>
    A.B.C.D
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nestedClass, objectDeclaration,
stringLiteral, typeAliasDeclaration */
