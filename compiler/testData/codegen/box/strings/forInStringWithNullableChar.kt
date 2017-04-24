// WITH_RUNTIME

fun useBoxedChar(ch: Char?) = ch!!

fun box(): String {
    var s = ""
    for (ch: Char? in "OK") {
        s += useBoxedChar(ch)
    }

    return s
}
