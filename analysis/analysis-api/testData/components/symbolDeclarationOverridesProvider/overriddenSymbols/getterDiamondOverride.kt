interface C {
    var foo: Int
}

interface D {
    var foo: Int
}

interface B : C, D

class A : B {
    override var foo: Int
        g<caret>et() = 0
        set(value) {}
}
