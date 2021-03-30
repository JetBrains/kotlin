// FILE: 1.kt
fun check1() = true

fun check2() = false

inline fun inlineMe1(fn: (String, String) -> String): String {
    return fn(if (check1()) return "O" else "1", return "2")
}

inline fun inlineMe2(fn: (String) -> String): String {
    return fn(if (check2()) return "3" else "K")
}

// FILE: 2.kt
fun box() = inlineMe1 { _, _ -> "FAIL1" } + inlineMe2 { it }
