package test

class A {
    object <!REDECLARATION!>Default<!>

    <!REDECLARATION!>class object<!>
}

class B {
    class object <!REDECLARATION!>Named<!>

    object <!REDECLARATION!>Named<!>
}

class C {
    class <!REDECLARATION!>Named<!>

    class object <!REDECLARATION!>Named<!>
}