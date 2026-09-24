// LANGUAGE: +FullValueClasses
value class A private constructor(val first: String, val second: String) {
    constructor(first: String) : this(first, "K")

    internal inline fun ok() = A(first, second).first + A(first, second).second
}

fun box(): String {
    return A("O").ok()
}
