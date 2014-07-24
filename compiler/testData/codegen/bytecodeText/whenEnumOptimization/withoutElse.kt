import kotlin.test.assertEquals

enum class Season {
    WINTER
    SPRING
    SUMMER
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

// 2 TABLESWITCH
// 1 @_DefaultPackage-withoutElse-.*\$WhenMappings\.class
