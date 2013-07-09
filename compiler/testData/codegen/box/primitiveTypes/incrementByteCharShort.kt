fun byteArg(b: Byte) {}
fun charArg(c: Char) {}
fun shortArg(s: Short) {}

fun box(): String {
    var b = 42.toByte()
    b++
    ++b
    byteArg(b)

    var c = 'x'
    c++
    ++c
    charArg(c)

    var s = 239.toShort()
    s++
    ++s
    shortArg(s)

    return "OK"
}
