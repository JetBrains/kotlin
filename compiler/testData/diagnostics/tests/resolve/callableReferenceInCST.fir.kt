// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun testWhen(x: Any?) {
    val y = when (x) {
        null -> ""
        else -> ::unresolved
    }
}

fun testWhenWithBraces(x: Any?) {
    val z = when(x) {
        null -> { "" }
        else -> { ::unresolved }
    }
}

fun testIf(x: Any?) {
    val y = if (x != null) ::unresolved else null
}

fun testIfWithBraces(x: Any?) {
    val z = if (x != null) { ::unresolved } else { null }
}

fun testElvis(x: Any?) {
    val y = x ?: ::unresolved
}

fun testExclExcl() {
    val y = :: unresolved!!
}

fun testTry() {
    val v = try { ::unresolved } catch (e: Exception) {}
}
