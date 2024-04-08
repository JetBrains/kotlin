// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER
}

data class Info(val season: Season?)

fun foo(anything: Any): Int =
    when ((anything as Info).season) {
        null -> 0
        Season.SPRING -> 1
        Season.SUMMER -> 2
        Season.AUTUMN -> 3
        Season.WINTER -> 4
    }