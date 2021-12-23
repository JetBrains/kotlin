enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN
}

fun foo1(x : Season?) : String {
    when(x) {
        Season.AUTUMN, Season.SPRING -> return "autumn_or_spring";
        Season.SUMMER, null -> return "summer_or_null"
        else -> return "other"
    }
}

fun foo2(x : Season?) : String {
    when(x) {
        Season.AUTUMN, Season.SPRING -> return "autumn_or_spring";
        Season.SUMMER -> return "summer"
        else -> return "other"
    }
}

// 2 TABLESWITCH
