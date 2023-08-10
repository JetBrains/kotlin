// LANGUAGE: +ContextReceivers

interface I {
    context(String, Int) fun foo()
}

class C1 : I {
    context(String, Int) override fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> : I {
    context(String) <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C3<!> : I {
    context(Int, String) <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C4<!> : I {
    context(String, Float) <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}
