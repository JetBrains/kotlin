// LANGUAGE: +FullValueClasses
// MODULE: lib
// FILE: A.kt
value class A private constructor(val first: String, val second: String) {
    constructor(first: String) : this(first, "K")

    internal inline fun ok() = A(first, second).first + A(first, second).second
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A("O").ok()
}
