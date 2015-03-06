// !DIAGNOSTICS: -MANY_DEFAULT_OBJECTS -REDECLARATION

// KT-3464 Front-end shouldn't allow override modifier in class declaration

<!ILLEGAL_MODIFIER!>override<!> class A {
    <!ILLEGAL_MODIFIER!>override<!> default object {}
    <!ILLEGAL_MODIFIER!>open<!> default object {}
    <!ILLEGAL_MODIFIER!>abstract<!> default object {}
    <!ILLEGAL_MODIFIER!>final<!> default object {}
}

<!ILLEGAL_MODIFIER!>override<!> object B1 {}
<!ILLEGAL_MODIFIER!>open<!> object B2 {}
<!ILLEGAL_MODIFIER!>abstract<!> object B3 {}
<!ILLEGAL_MODIFIER!>final<!> object B4 {}

<!ILLEGAL_MODIFIER!>override<!> enum class C {}
<!ILLEGAL_MODIFIER!>override<!> trait D {}
<!ILLEGAL_MODIFIER!>override<!> annotation class E
