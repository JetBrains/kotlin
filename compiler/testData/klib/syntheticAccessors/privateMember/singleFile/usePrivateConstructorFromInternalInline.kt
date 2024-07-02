class A private constructor(val s: String) {
    constructor(): this("")
    internal inline fun copy(s: String) = A(s)
}

fun box(): String {
    return A().copy("OK").s
}
