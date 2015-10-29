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
    var result = (1..4).map(::intFoo).joinToString()

    if (result != "5, 6, 7, 8") return "int:" + result

    result = (listOf<Short>(1, 2, 3, 4)).map(::shortFoo).joinToString()

    if (result != "5, 6, 7, 8") return "short:" + result

    result = (listOf<Byte>(1, 2, 3, 4)).map(::byteFoo).joinToString()

    if (result != "5, 6, 7, 8") return "byte:" + result

    result = ('a'..'d').map(::charFoo).joinToString()

    if (result != "5, 6, 7, 8") return "int:" + result
    return "OK"
}


