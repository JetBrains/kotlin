// ISSUE: KT-20677
// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// MODULE: m1-common

expect open class A

class C : <!NO_IMPLICIT_DEFAULT_CONSTRUCTOR_ON_EXPECT_CLASS!>A()<!> {
    fun f() {
        <!EXPECT_CLASS_AS_FUNCTION!>A<!>()
    }
}

expect interface I

// Make sure the diagnostic for interfaces is preserved, it has another kind
class E : I<!NO_CONSTRUCTOR!>()<!>

// MODULE: m1-jvm()()(m1-common)

class D : <!NO_IMPLICIT_DEFAULT_CONSTRUCTOR_ON_EXPECT_CLASS!>A()<!> {
    fun g() {
        <!EXPECT_CLASS_AS_FUNCTION!>A<!>()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, expect, functionDeclaration */
