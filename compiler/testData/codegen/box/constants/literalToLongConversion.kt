// ISSUE: KT-58132

const val hourInMilliseconds: Long = 60 * 60 * 1000

fun box(): String {
    val expected = 3600000L
    return if (hourInMilliseconds == expected) "OK" else "Fail: $hourInMilliseconds"
}
