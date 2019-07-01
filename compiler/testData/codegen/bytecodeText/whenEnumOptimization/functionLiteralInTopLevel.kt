import kotlin.test.assertEquals

enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN
}

fun foo(x : Season, block : (Season) -> String) = block(x)

fun box() : String {
    return foo(Season.SPRING) {
        x -> when (x) {
            Season.SPRING -> "OK"
            Season.SUMMER -> "fail" // redundant branch to force use of TABLESWITCH instead of IF_ICMPNE
            else -> "fail"
        }
    }
}

// 1 TABLESWITCH
