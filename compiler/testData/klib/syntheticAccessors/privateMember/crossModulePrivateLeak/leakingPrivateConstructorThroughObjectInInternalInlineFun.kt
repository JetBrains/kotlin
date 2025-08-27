// MODULE: lib
// FILE: lib.kt
open class A(val x: Long) {
    private constructor(x: Int): this(x.toLong())

    internal inline fun plus1() = object : A(x.toInt() + 1) {}
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    val result = A(1).plus1().x
    if (result != 2L) return result.toString()
    return "OK"
}