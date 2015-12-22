class A<T> {
    fun add(element: T) {}
}

public fun <R> foo(x: MutableCollection<in R>, block: () -> R) {
    x.add(block())
}
