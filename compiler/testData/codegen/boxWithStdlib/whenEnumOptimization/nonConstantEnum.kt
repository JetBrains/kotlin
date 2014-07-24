enum class Season {
    WINTER
    SPRING
    SUMMER
    AUTUMN
}

fun foo(): Season = Season.SPRING
fun bar(): Season = Season.SPRING

fun box() : String {
    when (foo()) {
        bar() -> return "OK"
        else -> return "fail"
    }
}
