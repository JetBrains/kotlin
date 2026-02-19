// FILE: lib.kt
inline fun inlineCall(block: (CharSequence, CharSequence) -> Unit) {
    for (i in 0..0) {
        val s = "K".takeIf { true }

        block(
            "O",
            s ?: continue
        )
    }
}

// FILE: main.kt
fun box(): String {
    inlineCall { a, b ->
        return a.toString() + b
    }
    return "Fail"
}
