// CHECK_CASES_COUNT: function=box$lambda count=0
// CHECK_IF_COUNT: function=box$lambda count=1 TARGET_BACKENDS=JS
// CHECK_IF_COUNT: function=box$lambda count=0 IGNORED_BACKENDS=JS

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
            else -> "fail"
        }
    }
}
