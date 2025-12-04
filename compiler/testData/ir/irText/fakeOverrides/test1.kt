open class Base<T> {
    open fun <A : List<T>> foo1(a: A) = Unit
    open fun foo2(a: List<T>) = Unit
}

class Der : Base<String>() {
    override fun <A : List<String>> foo1(a: A) = Unit
    override fun foo2(a: List<String>) = Unit
}
