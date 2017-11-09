open class T(open var <caret>foo: String = "")

open class X: T("") {
    override var foo: String
        get() = ""
        set(value: String) {}
}

open class Y(override var foo: String = ""): T(foo)