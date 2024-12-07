// FILE: 1.kt

class Box<T>(val value: T) {
    inline fun run(block: (T) -> Unit) {
        block(value)
    }
}

// FILE: 2.kt
fun box(): String {
    Box("OK").run { outer ->
        return outer
    }

    return "Fail"
}