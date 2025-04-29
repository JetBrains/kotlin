// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76365

interface I {
    fun foo()
}

interface J : I {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>() // NOT reported
}

enum class E1 : I {
    A, B, C;
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>() // NOT reported
}

enum class E2: I {
    A {
        override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>() // NOT reported
    };
}
