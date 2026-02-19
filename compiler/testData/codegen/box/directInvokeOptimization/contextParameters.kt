// LANGUAGE: +ContextParameters
// CHECK_BYTECODE_TEXT
// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY

context(x: Int)
fun Int.f(y: Int): Int {
    return { z: Int -> x + y + z + this }.invoke(2)
}

fun box(): String {
    val res = with(10) { 100.f(1000) }
    require(res == 1112) { res.toString() }
    return "OK"
}

// 0 invoke