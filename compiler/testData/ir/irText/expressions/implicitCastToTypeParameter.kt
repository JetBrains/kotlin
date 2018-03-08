inline fun <reified T : Any> Any.test1(): T? =
    if (this is T) this else null

interface Foo<T>

inline val <reified T : Any> Foo<T>.asT: T?
    get() = if (this is T) this else null