// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
class Box<T>(val value: T) {
    inline fun run(block: (T) -> Unit) {
        block(value)
    }
}

// FILE: 2.kt
fun box(): String {
    var result: String = "fail"
    Box("OK").run { outer ->
        val block = { result = outer }
        block()
    }
    return result
}