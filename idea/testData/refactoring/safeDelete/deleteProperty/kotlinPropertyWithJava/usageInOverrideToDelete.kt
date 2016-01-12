interface T {
    var <caret>foo: Int
}

class A(val t: T) : T {
    override var foo: Int
        get() = t.foo
        set(value) {
            t.foo = value
        }
}