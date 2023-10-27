class A {
    <!WRONG_ANNOTATION_TARGET!>@ann<!> init {}
    @<!UNRESOLVED_REFERENCE!>aaa<!> init {}
}

interface T {
    <!WRONG_ANNOTATION_TARGET!>@ann<!> <!ANONYMOUS_INITIALIZER_IN_INTERFACE!>init<!> {}
    @<!UNRESOLVED_REFERENCE!>aaa<!> <!ANONYMOUS_INITIALIZER_IN_INTERFACE!>init<!> {}
}

annotation class ann
