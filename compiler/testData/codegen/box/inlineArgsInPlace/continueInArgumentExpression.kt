// FULL_JDK
// WITH_RUNTIME

fun test(xs: List<String>, flag: Boolean = false): Map<String, String> {
    val result = linkedMapOf<String, String>()
    for (x in xs) {
        if (x.length > 3) continue
        result[x] = x.zap("OK", flag) ?: continue
    }
    return result
}

fun String.zap(y: String, flag: Boolean = false): String? {
    return if (flag || this == "x") y else null
}

fun box(): String {
    return test(listOf("a", "bcde", "x"))["x"]!!
}
