// FILE: test1.kt
class A
class B(val x: Int) {
    constructor(x: Int, y: Int): this(x + y)
}

// FILE: test2.kt
fun A() {}
fun B(x: Int) = x
fun B(x: Int, y: Int) = x + y