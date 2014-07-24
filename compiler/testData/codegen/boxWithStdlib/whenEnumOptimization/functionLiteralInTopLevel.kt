import kotlin.test.assertEquals

enum class Season {
    WINTER
    SPRING
    SUMMER
    AUTUMN
}

fun foo(x : Season, block : (Season) -> String) = block(x)

fun box() : String {
    return foo(Season.SPRING) {
        x -> when (x) {
            Season.SPRING -> "OK"
            else -> "fail"
        }
    }
}
