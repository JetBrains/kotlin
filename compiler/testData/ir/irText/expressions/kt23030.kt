// FIR_IDENTICAL
operator fun Int.compareTo(c: Char) = 0

fun testOverloadedCompareToCall(x: Int, y: Char) =
    x < y

fun testOverloadedCompareToCallWithSmartCast(x: Any, y: Any) =
    x is Int && y is Char && x < y

fun testEqualsWithSmartCast(x: Any, y: Any) =
    x is Int && y is Char && x == y

class C {
    operator fun Int.compareTo(c: Char) = 0

    fun testMemberExtensionCompareToCall(x: Int, y: Char) =
        x < y

    fun testMemberExtensionCompareToCallWithSmartCast(x: Any, y: Any) =
        x is Int && y is Char && x < y
}