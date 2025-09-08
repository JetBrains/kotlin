// FILE: A.kt

fun interface I {
    fun test(x: Int = 10, y: Int = 20) = process(x + y)
    fun process(a: Int) : Int
}

// FILE: B.kt

fun box() : String {
    val x = I { it * 2 }
    if (x.test() != 60) return "FAIL"
    return "OK"
}