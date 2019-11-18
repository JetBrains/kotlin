// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// CHECK_CASES_COUNT: function=bar1 count=3
// CHECK_IF_COUNT: function=bar1 count=0
// CHECK_CASES_COUNT: function=bar2 count=4
// CHECK_IF_COUNT: function=bar2 count=0

import kotlin.test.assertEquals

enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN
}

fun bar1(x : Season) : String {
    when (x) {
        Season.WINTER, Season.SPRING -> return "winter_spring"
        Season.SPRING -> return "spring"
        Season.SUMMER -> return "summer"
    }
    return "autumn"
}

fun bar2(x : Season) : String {
    when (x) {
        Season.WINTER, Season.SPRING -> return "winter_spring"
        Season.SPRING -> return "spring"
        Season.SUMMER -> return "summer"
        Season.AUTUMN -> return "autumn"
    }

    return "fail unknown"
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
