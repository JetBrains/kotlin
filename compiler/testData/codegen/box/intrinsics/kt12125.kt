// IGNORE_BACKEND_FIR: JVM_IR
fun test(i: Int): Int {
    return i
}

fun box(): String {
    var b = Byte.MAX_VALUE
    b++
    var result = test(b.toInt())
    if (result != Byte.MIN_VALUE.toInt()) return "fail 1: $result"

    var s = Short.MIN_VALUE
    s--
    result = test(s.toInt())
    if (result != Short.MAX_VALUE.toInt()) return "fail 2: $result"

    return "OK"
}