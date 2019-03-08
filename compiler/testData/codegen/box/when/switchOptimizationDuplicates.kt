fun foo(x: Int): Int {
    return when (x) {
        1, 1, 2 -> 1001
        1, 2 -> 1002
        1 -> 1003
        2 -> 1004
        3 -> 1005
        else -> 1006
    }
}

fun box(): String {
    if (foo(0) != 1006)
        return "0: " + foo(0)

    if (foo(1) != 1001)
        return "1: " + foo(1)

    if (foo(2) != 1001)
        return "2: " + foo(2)

    if (foo(3) != 1005)
        return "3: " + foo(3)

    if (foo(4) != 1006)
        return "4: " + foo(4)

    return "OK"
}