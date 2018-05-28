// !LANGUAGE: +InlineClasses
// !SKIP_METADATA_VERSION_CHECK
// WITH_UNSIGNED

fun box(): String {
    val u1 = 1u
    val u2 = 2u
    val u3 = u1 + u2
    if (u3.toInt() != 3) return "fail"

    val max = 0u.dec().toLong()
    val expected = Int.MAX_VALUE * 2L + 1
    if (max != expected) return "fail"

    return "OK"
}