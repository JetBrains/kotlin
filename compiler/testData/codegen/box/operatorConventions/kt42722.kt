// DONT_TARGET_EXACT_BACKEND: WASM
// WITH_RUNTIME

fun box(): String {
    val set = setOf<Int>(1, 2, 3, 4, 5)
    println(0 in set)
    println(1 in set)
    println(null in set)
    return "OK"
}
