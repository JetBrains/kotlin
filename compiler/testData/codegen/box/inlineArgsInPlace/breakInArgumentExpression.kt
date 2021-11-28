// FULL_JDK
// WITH_STDLIB

fun test(xs: List<String>): Map<String, String> {
    val result = linkedMapOf<String, String>()
    for (x in xs) {
        result[x] = x.zap("OK") ?: break
    }
    return result
}

fun String.zap(y: String): String? {
    return if (this == "x") y else null
}

fun box(): String {
    return test(listOf("x", "bcde", "a"))["x"]!!
}
