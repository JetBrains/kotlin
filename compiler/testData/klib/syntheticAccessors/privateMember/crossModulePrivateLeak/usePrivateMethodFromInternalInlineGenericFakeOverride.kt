// MODULE: lib
// FILE: A.kt
open class A<T> {
    private fun <U> privateMethod(o: T, k: U) = o.toString() + k.toString()

    internal inline fun <U> internalInlineMethod(o: T, k: U) = privateMethod<U>(o, k)
}

class B : A<Char>()

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String {
    return B().internalInlineMethod<String>('O', "K")
}
