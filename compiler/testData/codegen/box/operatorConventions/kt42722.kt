// DONT_TARGET_EXACT_BACKEND: WASM
// WITH_RUNTIME

fun box(): String {
    val set = setOf<Int>(1, 2, 3, 4, 5)
    val x = 0 in set
    val y = 1 in set
    val z = null in set
    return "OK"
}
