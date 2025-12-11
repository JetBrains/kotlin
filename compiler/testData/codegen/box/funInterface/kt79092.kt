// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.2
// ^^^ KT-79092 is fixed in 2.3.0-Beta1
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