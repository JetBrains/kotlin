// !DIAGNOSTICS: -MANY_COMPANION_OBJECTS -REDECLARATION

// KT-3464 Front-end shouldn't allow override modifier in class declaration

<!WRONG_MODIFIER_TARGET!>override<!> class A {
    <!WRONG_MODIFIER_TARGET!>override<!> companion object {}
    <!WRONG_MODIFIER_TARGET!>open<!> companion object {}
    <!WRONG_MODIFIER_TARGET!>abstract<!> companion object {}
    <!WRONG_MODIFIER_TARGET!>final<!> companion object {}
}

<!WRONG_MODIFIER_TARGET!>override<!> object B1 {}
<!WRONG_MODIFIER_TARGET!>open<!> object B2 {}
<!WRONG_MODIFIER_TARGET!>abstract<!> object B3 {}
<!WRONG_MODIFIER_TARGET!>final<!> object B4 {}

<!WRONG_MODIFIER_TARGET!>override<!> enum class C {}
<!WRONG_MODIFIER_TARGET!>override<!> interface D {}
<!WRONG_MODIFIER_TARGET!>override<!> annotation class E