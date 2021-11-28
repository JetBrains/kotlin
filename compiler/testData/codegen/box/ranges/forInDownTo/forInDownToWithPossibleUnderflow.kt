// WITH_STDLIB

const val MB = Byte.MIN_VALUE
const val MS = Short.MIN_VALUE
const val MI = Int.MIN_VALUE
const val ML = Long.MIN_VALUE
const val MC = Char.MIN_VALUE

fun testByte() {
    var s = ""
    var t = 0
    for (i in MB + 1 downTo MB) {
        ++t
        s += i
        if (t > 2) throw Exception("too many iterations: $t")
    }
    if (s != "-127-128") throw Exception(s)
}

fun testShort() {
    var s = ""
    var t = 0
    for (i in MS + 1 downTo MS) {
        ++t
        s += i
        if (t > 2) throw Exception("too many iterations: $t")
    }
    if (s != "-32767-32768") throw Exception(s)
}

fun testInt() {
    var s = ""
    var t = 0
    for (i in MI + 1 downTo MI) {
        ++t
        s += i
        if (t > 2) throw Exception("too many iterations: $t")
    }
    if (s != "-2147483647-2147483648") throw Exception(s)
}

fun testLong() {
    var s = ""
    var t = 0
    for (i in ML + 1L downTo ML) {
        ++t
        s += i
        if (t > 2) throw Exception("too many iterations: $t")
    }
    if (s != "-9223372036854775807-9223372036854775808" &&
            s != "-9223372036854776000-9223372036854776000" // JS
    ) throw Exception(s)
}

fun testChar() {
    var s = ""
    var t = 0
    for (i in (MC.toInt() + 1).toChar() downTo MC) {
        ++t
        s += i.toInt()
        if (t > 2) throw Exception("too many iterations: $t")
    }
    if (s != "10") throw Exception(s)
}

fun box(): String {
    testByte()
    testShort()
    testInt()
    testLong()
    testChar()

    return "OK"
}
