// IGNORE_BACKEND_K1: ANY

fun foo(vararg ii: Int) = 1
fun foo(vararg ss: String) = 2
fun foo(i: Int) = 3

val fn1: (Int) -> Int = ::foo
val fn2: (IntArray) -> Int = ::foo
val fn3: (Int, Int) -> Int = ::foo // K1 reports NONE_APPLICABLE
val fn4: (Array<String>) -> Int = ::foo

fun box(): String {
    if (fn1(1) != 3) return "FAIL 1"
    if (fn2(intArrayOf(1)) != 1) return "FAIL 2"
    if (fn3(1, 2) != 1) return "FAIL 3"
    if (fn4(arrayOf("")) != 2) return "FAIL 4"

    return "OK"
}