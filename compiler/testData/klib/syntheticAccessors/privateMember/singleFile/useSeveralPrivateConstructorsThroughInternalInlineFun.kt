class A private constructor(val s: String) {
    constructor(): this("")
    private constructor(a: String, b: String): this(a + b)
    private constructor(a: Char): this(a.toString())

    internal inline fun complexCopy(s: String): A {
        A()
        A(s)
        A(' ')
        return A(s, s)
    }
}

fun box(): String {
    return A().complexCopy("OK").s
}
