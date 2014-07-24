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

// 1 LOOKUPSWITCH
// 1 @_DefaultPackage-functionLiteralInTopLevel-[a-z0-9]+\$WhenMappings.class
