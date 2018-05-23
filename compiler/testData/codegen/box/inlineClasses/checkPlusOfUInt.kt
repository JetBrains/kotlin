// !LANGUAGE: +InlineClasses
// !SKIP_METADATA_VERSION_CHECK
// WITH_UNSIGNED

@Suppress("INVISIBLE_MEMBER")
fun box(): String {
    val u1 = UInt(1)
    val u2 = UInt(2)
    val u3 = u1 + u2
    return if (u3.toInt() == 3) "OK" else "fail"
}