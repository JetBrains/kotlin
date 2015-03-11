import kotlin.test.assertEquals

class A {
    default object {
        enum class Season {
            WINTER
            SPRING
            SUMMER
            AUTUMN
        }
    }
}

fun foo(x : A.Default.Season) : String {
    return when (x) {
        A.Default.Season.WINTER -> "winter"
        A.Default.Season.SPRING -> "spring"
        A.Default.Season.SUMMER -> "summer"
        else -> "other"
    }
}

fun box() : String {
    assertEquals("winter", foo(A.Default.Season.WINTER))
    assertEquals("spring", foo(A.Default.Season.SPRING))
    assertEquals("summer", foo(A.Default.Season.SUMMER))
    assertEquals("other", foo(A.Default.Season.AUTUMN))
    return "OK"
}
