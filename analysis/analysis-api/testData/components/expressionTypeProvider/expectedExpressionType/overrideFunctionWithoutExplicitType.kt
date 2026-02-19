open class OpenClass {
    open fun compute(): Int = 42
}

class DerivedClass : OpenClass() {
    override fun compute() = resul<caret>t
}
