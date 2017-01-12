fun box(): String {
    val a1: Any = 1.toByte().plus(1)
    val a2: Any = 1.toShort().plus(1)
    val a3: Any = 1.plus(1)
    val a4: Any = 1L.plus(1)
    val a5: Any = 1.0.plus(1)
    val a6: Any = 1f.plus(1)
    val a7: Any = 'A'.plus(1)
    val a8: Any = 'B'.minus('A')

    if (a1 !is Int || a1 != 2) return "fail 1"
    if (a2 !is Int || a2 != 2) return "fail 2"
    if (a3 !is Int || a3 != 2) return "fail 3"
    if (a4 !is Long || a4 != 2L) return "fail 4"
    if (a5 !is Double || a5 != 2.0) return "fail 5"
    if (a6 !is Float || a6 != 2f) return "fail 6"
    if (a7 !is Char || a7 != 'B') return "fail 7"
    if (a8 !is Int || a8 != 1) return "fail 8"

    return "OK"
}