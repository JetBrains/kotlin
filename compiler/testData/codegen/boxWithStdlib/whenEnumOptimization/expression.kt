import kotlin.test.assertEquals

enum class Season {
    WINTER
    SPRING
    SUMMER
    AUTUMN
}

fun bar1(x : Season) : String {
    return when (x) {
        Season.WINTER, Season.SPRING -> "winter_spring"
        Season.SUMMER -> "summer"
        else -> "autumn"
    }
}

fun bar2(x : Season) : String {
    return when (x) {
        Season.WINTER, Season.SPRING -> "winter_spring"
        Season.SUMMER -> "summer"
        Season.AUTUMN -> "autumn"
    }
}

fun box() : String {
    assertEquals("winter_spring", bar1(Season.WINTER))
    assertEquals("winter_spring", bar1(Season.SPRING))
    assertEquals("summer", bar1(Season.SUMMER))
    assertEquals("autumn", bar1(Season.AUTUMN))

    assertEquals("winter_spring", bar2(Season.WINTER))
    assertEquals("winter_spring", bar2(Season.SPRING))
    assertEquals("summer", bar2(Season.SUMMER))
    assertEquals("autumn", bar2(Season.AUTUMN))
    return "OK"
}
