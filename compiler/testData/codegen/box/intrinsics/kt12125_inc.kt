// IGNORE_BACKEND_FIR: JVM_IR
fun test(i: Int): Int {
    return i
}

fun box(): String {
    var b = Byte.MAX_VALUE
    var result = test(b.inc().toInt())
    if (result != Byte.MIN_VALUE.toInt()) return "fail 1: $result"

    var s = Short.MAX_VALUE
    result = test(s.inc().toInt())
    if (result != Short.MIN_VALUE.toInt()) return "fail 2: $result"

    return "OK"
}