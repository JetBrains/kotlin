// If this test will start to fail after KT-69666, then it can be safely removed
// FILE: A.kt
class A<T> private constructor(val s: T) {
    constructor(): this("" as T)
    internal inline fun copy(s: String) = A(s)
}

// FILE: main.kt
fun box(): String {
    return A<String>().copy("OK").s
}
