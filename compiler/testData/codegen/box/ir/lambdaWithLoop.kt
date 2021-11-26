// WITH_STDLIB

fun fooInt(b: (Int, Int) -> String): String {
    return b(3, 5)
}

fun fooULong(b: (ULong, ULong) -> String): String {
    return b(3UL, 7UL)
}

fun barInt(i: Int): String {
    return "FAIINTL1".get(i).toString()
}

fun barULong(l: ULong): String {
    return "FAIULONGL2".get(l.toInt()).toString()
}

fun testInt(): String {
    return fooInt { from, to ->
        var r = ""
        for (index in from..to) {
            r += barInt(index)
        }
        r
    }
}

fun testULong(): String {
    return fooULong { from, to ->
        var r = ""
        for (index in from..to) {
            r += barULong(index)
        }
        r
    }
}

fun box(): String {

    val r1 = testInt()

    if (r1 != "INT") return "FAIL1: $r1"

    val r2 = testULong()

    if (r2 != "ULONG") return "FAIL2: $r2"

    return "OK"
}