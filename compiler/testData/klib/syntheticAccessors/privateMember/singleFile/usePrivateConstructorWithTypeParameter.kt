class A<T> private constructor(val s: T) {
    constructor(): this("" as T)
    internal inline fun copy(s: String) = A(s)
}

fun box(): String {
    return A<String>().copy("OK").s
}
