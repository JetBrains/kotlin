// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
package test

class A {
    object <!REDECLARATION!>Companion<!>

    companion <!REDECLARATION!>object<!>
}

class B {
    companion object <!REDECLARATION!>Named<!>

    object <!REDECLARATION!>Named<!>
}

class C {
    class <!REDECLARATION!>Named<!>

    companion object <!REDECLARATION!>Named<!>
}