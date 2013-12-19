package test

annotation class A
annotation class B

var foo: Int
    get() = 42
    set([A B] value) {}
