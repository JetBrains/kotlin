// WITH_STDLIB

fun test() = uintArrayOf(1u).size

fun box(): String {
    val test = test()
    if (test != 1) return "Failed: $test"
    return "OK"
}