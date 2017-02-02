fun box(): String {
    val shouldBeTrue = 555555555555555555L in 123456789123456789L..987654321987654321L
    if (!shouldBeTrue) return "Fail 1"

    val shouldBeFalse = 5000000000L in 6000000000L..9000000000L
    if (shouldBeFalse) return "Fail 2"

    if (123123123123L !in 100100100100L..200200200200L) return "Fail 3"

    return when (9876543210) {
        in 2000000000L..3333333333L -> "Fail 4"
        !in 8888888888L..9999999999L -> "Fail 5"
        else -> "OK"
    }
}
