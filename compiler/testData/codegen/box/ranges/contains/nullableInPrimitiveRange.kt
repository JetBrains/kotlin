// WITH_STDLIB

val x: Int? = 42
fun xFun(): Int? = 42

val n: Int? = null
val nProp: Int?
    get() = null

fun box(): String {
    if (x in 0..2) return "Fail in"
    if (!(x !in 0..2)) return "Fail !in"

    if (xFun() in 0..2) return "Fail in function"
    if (!(xFun() !in 0..2)) return "Fail !in function"

    if (n in 0..2) return "Fail in null"
    if (!(n !in 0..2)) return "Fail !in null"

    if (nProp in 0..2) return "Fail in null property"
    if (!(nProp !in 0..2)) return "Fail !in null property"

    val v: Int? = 10
    if (v in 0..2) return "Fail in variable"
    if (!(v !in 0..2)) return "Fail !in variable"

    val nul: Int? = null
    if (nul in 0..2) return "Fail in null variable"
    if (!(nul !in 0..2)) return "Fail !in null variable"

    if (null in 0..2) return "Fail in null const"
    if (!(null !in 0..2)) return "Fail !in null const"

    if ({ x }() in 0..2) return "Fail in complex"
    if (!({ x }() !in 0..2)) return "Fail !in complex"

    return "OK"
}