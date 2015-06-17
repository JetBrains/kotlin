sealed class Season {
    object Warm: Season()
    object Cold: Season()
}

fun foo(): Season = Season.Warm

fun box() = when(foo()) {
    Season.Warm -> "OK"
    Season.Cold -> "Fail: Cold, should be Warm"
}
