open class A<T> {
    private fun <U> privateMethod(o: T, k: U) = o.toString() + k.toString()

    internal inline fun <U> internalInlineMethod(o: T, k: U) = privateMethod<U>(o, k)
}

class B : A<Char>()

fun box(): String {
    return B().internalInlineMethod<String>('O', "K")
}
