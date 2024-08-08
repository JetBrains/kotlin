// If this test will start to fail after KT-69666, then it can be safely removed
// MODULE: lib
// FILE: A.kt
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

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().complexCopy("OK").s
}
