fun foo(x: Int): String {
    return when (x) {
        2_147_483_647 -> "MAX"
        -2_147_483_648 -> "MIN"
        else -> "else"
    }
}

fun box(): String {
    if (foo(0) != "else")
        return "0: " + foo(0).toString()

    if (foo(Int.MAX_VALUE) != "MAX")
        return "Int.MAX_VALUE: " + foo(Int.MAX_VALUE).toString()

    if (foo(Int.MIN_VALUE) != "MIN")
        return "Int.MIN_VALUE: " + foo(Int.MIN_VALUE).toString()

    return "OK"
}