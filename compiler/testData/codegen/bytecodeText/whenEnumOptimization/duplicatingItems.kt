import kotlin.test.assertEquals

enum class Season {
    WINTER
    SPRING
    SUMMER
    AUTUMN
}

fun bar(x : Season) : String {
    when (x) {
        Season.WINTER, Season.SPRING -> return "winter_spring"
        Season.SUMMER, Season.SPRING -> return "summer"
        else -> return "autumn"
    }
}

// 1 TABLESWITCH
