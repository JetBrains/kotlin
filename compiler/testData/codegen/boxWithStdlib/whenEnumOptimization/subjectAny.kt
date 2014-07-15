import kotlin.test.assertEquals

enum class Season {
    WINTER
    SPRING
    SUMMER
    AUTUMN
}

fun foo(x : Any) : String {
    return when (x) {
        Season.WINTER -> "winter"
        Season.SPRING -> "spring"
        Season.SUMMER -> "summer"
        else -> "other"
    }
}

fun box() : String {
    assertEquals("winter", foo(Season.WINTER))
    assertEquals("spring", foo(Season.SPRING))
    assertEquals("summer", foo(Season.SUMMER))
    assertEquals("other", foo(Season.AUTUMN))
    assertEquals("other", foo(123))
    return "OK"
}
