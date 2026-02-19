open class OpenClass {
    open val value: String = "test"
}

class DerivedClass : OpenClass() {
    override val value get() = tex<caret>t
}
