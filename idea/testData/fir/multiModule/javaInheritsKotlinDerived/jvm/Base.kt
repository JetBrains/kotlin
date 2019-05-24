open class Base {
    open val some: String get() = "Base"
}

open class Derived : Base() {
    override val some: String get() = "Derived"
}