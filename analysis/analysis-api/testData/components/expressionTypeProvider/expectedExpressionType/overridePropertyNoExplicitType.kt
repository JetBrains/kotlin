open class OpenClass {
    open val test: Boolean = true
}

class DerivedClass : OpenClass() {
    override val test = some<caret>Prefix
}
