// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +ContextParameters

interface I {
    context(_: String, _: Int) fun foo()
}

class C1 : I {
    context(_: String, _: Int) override fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> : I {
    context(_: String) <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C3<!> : I {
    context(_: Int, _: String) <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C4<!> : I {
    context(_: String, _: Float) <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration,
override */
