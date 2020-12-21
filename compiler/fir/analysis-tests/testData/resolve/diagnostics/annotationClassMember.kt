annotation class A() {
    <!ANNOTATION_CLASS_MEMBER!><!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED{PSI}!>constructor(s: Nothing?)<!><!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED{LT}!><!> {}<!>
    <!ANNOTATION_CLASS_MEMBER!>init {}<!>
    <!ANNOTATION_CLASS_MEMBER!>fun foo() {}<!>
    <!ANNOTATION_CLASS_MEMBER!>val bar: Nothing?<!>
    <!ANNOTATION_CLASS_MEMBER!>val baz get() = Unit<!>
}
