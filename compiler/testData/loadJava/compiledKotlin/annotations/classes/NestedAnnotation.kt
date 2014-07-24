package test

class A {
    annotation class Anno
}

A.Anno class B {
    A.Anno fun f() {}
}