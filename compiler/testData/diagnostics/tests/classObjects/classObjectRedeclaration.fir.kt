// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
package test

class A {
    <!REDECLARATION!>object Companion<!>

    companion <!REDECLARATION!>object<!>
}

class B {
    companion <!REDECLARATION!>object Named<!>

    <!REDECLARATION!>object Named<!>
}

class C {
    <!REDECLARATION!>class Named<!>

    companion <!REDECLARATION!>object Named<!>
}