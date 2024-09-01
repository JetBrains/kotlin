// ISSUE: KT-68803

interface Foo<out T> {
    val foo: T
}

interface ImplA<T> {
    val fooOrNull: T?
        get() = if (this is Foo<*>) {
            @Suppress("UNCHECKED_CAST")
            foo as T
        } else null
}
