open class A {
    open var <caret>foo: String
        get() {
            return "A"
        }
        set(value: String) {
            println()
        }
}

interface Z {
    var foo: String
}

class B: A(), Z {
    override var foo: String
        get() {
            return "B"
        }
        set(value: String) {
            println()
        }
}