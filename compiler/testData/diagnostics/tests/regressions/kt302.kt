// KT-302 Report an error when inheriting many implementations of the same member

package kt302

trait A {
    open fun foo() {}
}

trait B {
    open fun foo() {}
}

class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>C<!> : A, B {} //should be error here
