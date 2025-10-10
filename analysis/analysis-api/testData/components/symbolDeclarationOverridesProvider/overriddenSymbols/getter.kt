interface Base {
    var foo: Int
}

class Derived : Base() {
    override var foo: Int
        g<caret>et() = 0
        set(value) {}
}
