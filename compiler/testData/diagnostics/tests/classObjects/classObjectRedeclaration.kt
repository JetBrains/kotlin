package test

class A {
    object <!REDECLARATION!>Default<!>

    default <!REDECLARATION!>object<!>
}

class B {
    default object <!REDECLARATION!>Named<!>

    object <!REDECLARATION!>Named<!>
}

class C {
    class <!REDECLARATION!>Named<!>

    default object <!REDECLARATION!>Named<!>
}