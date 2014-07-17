import kotlin.test.assertEquals

enum class Season {
    WINTER
    SPRING
    SUMMER
    AUTUMN
}

fun foo1(x : Season?) : String {
    when(x) {
        Season.AUTUMN, Season.SPRING -> return "autumn_or_spring";
        Season.SUMMER, null -> return "summer_or_null"
    }

    return "other"
}

fun foo2(x : Season?) : String {
    when(x) {
        Season.AUTUMN, Season.SPRING -> return "autumn_or_spring";
        Season.SUMMER -> return "summer"
    }

    return "other"
}

fun box() : String {
    assertEquals("autumn_or_spring", foo1(Season.AUTUMN))
    assertEquals("autumn_or_spring", foo1(Season.SPRING))
    assertEquals("summer_or_null", foo1(Season.SUMMER))
    assertEquals("summer_or_null", foo1(null))

    assertEquals("autumn_or_spring", foo2(Season.AUTUMN))
    assertEquals("autumn_or_spring", foo2(Season.SPRING))
    assertEquals("summer", foo2(Season.SUMMER))
    assertEquals("other", foo2(null))

    return "OK"
}
