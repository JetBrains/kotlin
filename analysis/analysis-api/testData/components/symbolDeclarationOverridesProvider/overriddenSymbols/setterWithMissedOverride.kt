interface Base {
    var foo: Int
}

class Derived : Base {
    var foo: Int
        get() = 0
        s<caret>et(value) {}
}
