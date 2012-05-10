package test

open class BaseClass() {
    open val shape = "square"
}

open class Subclass() : BaseClass() {
    override open val shape = "circle"
}
