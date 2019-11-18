// IGNORE_BACKEND_FIR: JVM_IR
// CHECK_CASES_COUNT: function=box$lambda count=0
// CHECK_IF_COUNT: function=box$lambda count=1

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
