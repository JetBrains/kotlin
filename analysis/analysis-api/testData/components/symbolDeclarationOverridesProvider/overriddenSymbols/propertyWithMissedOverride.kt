interface Base {
    var foo: Int
}

class Derived : Base() {
    var f<caret>oo: Int = 1
}
