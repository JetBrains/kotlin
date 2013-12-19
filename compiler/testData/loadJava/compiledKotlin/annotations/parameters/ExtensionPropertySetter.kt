package test

annotation class A

class Class {
    var Int.foo: Int
        get() = this
        set([A] value) {}
}
