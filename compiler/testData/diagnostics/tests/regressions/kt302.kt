// KT-302 Report an error when inheriting many implementations of the same member

package kt302

interface A {
    open fun foo() {}
}

interface B {
    open fun foo() {}
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class C<!> : A, B {} //should be error here