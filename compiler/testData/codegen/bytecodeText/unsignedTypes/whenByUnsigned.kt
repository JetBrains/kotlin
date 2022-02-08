// WITH_STDLIB

const val M1: UInt = 2147483648u

fun testUInt1(x: UInt) =
    when (x) {
        0u -> "none"
        1u -> "one"
        2u -> "two"
        3u -> "three"
        else -> "many"
    }

fun testUInt2(x: UInt) =
    when (x) {
        0u -> "none"
        1u -> "one"
        2u -> "two"
        3u -> "three"
        M1 -> "M1"
        else -> "many"
    }

// 2 SWITCH
// ^ This captures both TABLESWITCH and LOOKUPSWITCH; we don't care about nuances of *SWITCH instruction generation here.

// 0 IF_ICMP