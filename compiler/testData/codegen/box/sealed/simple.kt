// IGNORE_BACKEND_FIR: JVM_IR
sealed class Season {
    class Warm: Season()
    class Cold: Season()
}

fun foo(): Season = Season.Warm()

fun box() = when(foo()) {
    is Season.Warm -> "OK"
    is Season.Cold -> "Fail: Cold, should be Warm"
}
