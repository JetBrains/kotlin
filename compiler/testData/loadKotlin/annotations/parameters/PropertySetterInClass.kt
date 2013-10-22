package test

annotation class A

class Class {
    var foo: Int
        get() = 42
        set([A] value) {}
}
