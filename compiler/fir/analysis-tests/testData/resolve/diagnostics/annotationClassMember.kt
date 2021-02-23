annotation class A() {
    <!ANNOTATION_CLASS_MEMBER!><!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(s: Nothing?)<!> {}<!>
    <!ANNOTATION_CLASS_MEMBER!>init {}<!>
    <!ANNOTATION_CLASS_MEMBER!>fun foo() {}<!>
    <!ANNOTATION_CLASS_MEMBER, MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val bar: Nothing?<!>
    <!ANNOTATION_CLASS_MEMBER!>val baz get() = Unit<!>
}
