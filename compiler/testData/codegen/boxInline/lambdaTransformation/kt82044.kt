// MODULE: a
// FILE: a.kt

inline fun <T, R> T.myLet(block: (T) -> R): R {
    return block(this)
}

// MODULE: b(a)
// FILE: test.kt

fun box(): String {
    "O".myLet {
        val result = it + "K"
        return result
    }
}
