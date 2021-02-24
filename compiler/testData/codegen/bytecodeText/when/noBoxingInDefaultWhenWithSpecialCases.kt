fun testInt(i: Int?) =
    when (i) {
        0 -> "zero"
        42 -> "magic"
        else -> "other"
    }

fun testLong(i: Long?) =
    when (i) {
        0L -> "zero"
        42L -> "magic"
        else -> "other"
    }

// 0 valueOf
// 0 Integer.valueOf
// 0 Long.valueOf
// 0 areEqual