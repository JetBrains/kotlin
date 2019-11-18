// IGNORE_BACKEND_FIR: JVM_IR
open class T(var value: Int) {}

fun plusAssign(): T {

    operator fun T.plusAssign(s: Int) {
        value += s
    }

    var t  = T(1)
    t += 1

    return t
}

fun box(): String {
    val result = plusAssign().value
    if (result != 2) return "fail 1: $result"

    return "OK"
}
