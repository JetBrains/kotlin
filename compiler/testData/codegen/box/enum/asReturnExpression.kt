// http://youtrack.jetbrains.com/issue/KT-2167
// KT-55828
// IGNORE_BACKEND_K2: NATIVE

enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN
}

fun foo() = Season.SPRING

fun box() =
    if (foo() == Season.SPRING) "OK"
    else "fail"
