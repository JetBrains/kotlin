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

// 2 TABLESWITCH
// 1 @_DefaultPackage-expression-.*\$WhenMappings\.class
