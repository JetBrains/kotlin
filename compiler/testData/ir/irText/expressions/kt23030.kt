operator fun Int.compareTo(c: Char) = 0

fun testOverloadedCompareToCall(x: Int, y: Char) =
    x < y

fun testOverloadedCompareToCallWithSmartCast(x: Any, y: Any) =
    x is Int && y is Char && x < y

// TODO: FE1.0 allows comparison of incompatible type after smart cast (KT-46383) but FIR rejects it. We need to figure out a transition plan.
//fun testEqualsWithSmartCast(x: Any, y: Any) =
//    x is Int && y is Char && x == y

class C {
    operator fun Int.compareTo(c: Char) = 0

    fun testMemberExtensionCompareToCall(x: Int, y: Char) =
        x < y

    fun testMemberExtensionCompareToCallWithSmartCast(x: Any, y: Any) =
        x is Int && y is Char && x < y
}