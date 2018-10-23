// WITH_RUNTIME

val x: Int? = 42
val n: Int? = null

fun box(): String {
    if (x in 0..2) return "Fail in"
    if (!(x !in 0..2)) return "Fail !in"

    if (n in 0..2) return "Fail in null"
    if (!(n !in 0..2)) return "Fail !in null"

    return "OK"
}