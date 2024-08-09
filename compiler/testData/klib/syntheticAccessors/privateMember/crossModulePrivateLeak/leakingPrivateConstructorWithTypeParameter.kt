// MODULE: lib
// FILE: A.kt
class A<T> private constructor(val s: T) {
    constructor(): this("" as T)
    internal inline fun copy(s: String) = A(s)
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A<String>().copy("OK").s
}
