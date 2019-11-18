// IGNORE_BACKEND_FIR: JVM_IR
sealed class Season {
    object Warm: Season()
    object Cold: Season()
}

fun foo(): Season = Season.Warm

fun box() = when(foo()) {
    Season.Warm -> "OK"
    Season.Cold -> "Fail: Cold, should be Warm"
}
