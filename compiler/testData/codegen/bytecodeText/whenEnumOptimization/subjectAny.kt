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

// 0 TABLESWITCH
// 0 LOOKUPSWITCH
