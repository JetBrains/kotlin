// LANGUAGE: +FullValueClasses
// FILE: A.kt
value class A private constructor(val first: String, val second: String) {
    constructor(first: String) : this(first, "K")

    internal inline fun ok() = A(first, second).first + A(first, second).second
}

// FILE: main.kt
fun box(): String {
    return A("O").ok()
}
