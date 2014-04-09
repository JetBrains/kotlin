fun intFoo(x: Int): Int {
    return when (x) {
        1 -> 5
        2 -> 6
        3 -> 7
        else -> 8
    }
}

fun shortFoo(x: Short): Int {
    return when (x) {
        1.toShort() -> 5
        2.toShort() -> 6
        3.toShort() -> 7
        else -> 8
    }
}

fun byteFoo(x: Byte): Int {
    return when (x) {
        1.toByte() -> 5
        2.toByte() -> 6
        3.toByte() -> 7
        else -> 8
    }
}

fun charFoo(x: Char): Int {
    return when (x) {
        'a' -> 5
        'b' -> 6
        'c' -> 7
        else -> 8
    }
}

fun box(): String {
    var result = (1..4).map(::intFoo).makeString()

    if (result != "5, 6, 7, 8") return "int:" + result

    result = (1.toShort()..4.toShort()).map(::shortFoo).makeString()

    if (result != "5, 6, 7, 8") return "short:" + result

    result = (1.toByte()..4.toByte()).map(::byteFoo).makeString()

    if (result != "5, 6, 7, 8") return "byte:" + result

    result = ('a'..'d').map(::charFoo).makeString()

    if (result != "5, 6, 7, 8") return "int:" + result
    return "OK"
}


