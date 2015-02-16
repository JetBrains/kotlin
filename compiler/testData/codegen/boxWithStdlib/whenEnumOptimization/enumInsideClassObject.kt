import kotlin.test.assertEquals

class A {
    class object {
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
        A.Season.WINTER -> "winter"
        A.Season.SPRING -> "spring"
        A.Season.SUMMER -> "summer"
        else -> "other"
    }
}

fun box() : String {
    assertEquals("winter", foo(A.Season.WINTER))
    assertEquals("spring", foo(A.Season.SPRING))
    assertEquals("summer", foo(A.Season.SUMMER))
    assertEquals("other", foo(A.Season.AUTUMN))
    return "OK"
}
