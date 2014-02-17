class A {
    ann {}
    [ann] {}
    <!UNRESOLVED_REFERENCE!>aaa<!> {}
    [<!UNRESOLVED_REFERENCE!>aaa<!>] {}
}

trait T {
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!>ann {}<!>
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!>[ann] {}<!>
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!><!UNRESOLVED_REFERENCE!>aaa<!> {}<!>
    <!ANONYMOUS_INITIALIZER_IN_TRAIT!>[<!UNRESOLVED_REFERENCE!>aaa<!>] {}<!>
}

annotation class ann