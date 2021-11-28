// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt
inline fun List<String?>.forEachNotNull(s: String, fn: (String, String) -> Unit) {
    for (x in this) {
        fn(s, x ?: continue)
    }
}

inline fun List<String?>.forEachUntilNull(s: String, fn: (String, String) -> Unit) {
    for (x in this) {
        fn(s, x ?: break)
    }
}
// FILE: 2.kt
fun box(): String {
    var res = ""
    listOf("O", null, "K").forEachNotNull("|") { a, b ->
        res += a + b
    }
    if (res != "|O|K") return res
    res = ""

    listOf("O", null, "K").forEachUntilNull("|") { a, b ->
        res += a + b
    }
    if (res != "|O") return res
    return "OK"
}
