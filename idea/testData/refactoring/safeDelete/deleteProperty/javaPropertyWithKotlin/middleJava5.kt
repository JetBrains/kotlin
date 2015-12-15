interface B {
    public var foo: String
}

class D: B {
    public override var foo: String
        get() {
            return "D"
        }
        set(value: String) {
            println()
        }
}