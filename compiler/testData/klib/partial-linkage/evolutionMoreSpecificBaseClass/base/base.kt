open class X {
    open val bar: String = "base class"
}

open class Y: X() {
    override val bar: String = "child class"
}

