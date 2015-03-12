class A {
    ann init {}
    [ann] init {}
    <!UNRESOLVED_REFERENCE!>aaa<!> init {}
    [<!UNRESOLVED_REFERENCE!>aaa<!>] init {}
}

trait T {
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!>ann init {}<!>
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!>[ann] init {}<!>
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!><!UNRESOLVED_REFERENCE!>aaa<!> init {}<!>
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!>[<!UNRESOLVED_REFERENCE!>aaa<!>] init {}<!>
}

annotation class ann