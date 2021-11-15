// WITH_STDLIB
// CHECK_CASES_COUNT: function=foo count=3
// CHECK_IF_COUNT: function=foo count=0

import kotlin.test.assertEquals

class A {
    companion object {
        enum class Season {
            WINTER,
            SPRING,
            SUMMER,
            AUTUMN
        }
    }
}

fun foo(x : A.Companion.Season) : String {
    return when (x) {
        A.Companion.Season.WINTER -> "winter"
        A.Companion.Season.SPRING -> "spring"
        A.Companion.Season.SUMMER -> "summer"
        else -> "other"
    }
}

fun box() : String {
    assertEquals("winter", foo(A.Companion.Season.WINTER))
    assertEquals("spring", foo(A.Companion.Season.SPRING))
    assertEquals("summer", foo(A.Companion.Season.SUMMER))
    assertEquals("other", foo(A.Companion.Season.AUTUMN))
    return "OK"
}
