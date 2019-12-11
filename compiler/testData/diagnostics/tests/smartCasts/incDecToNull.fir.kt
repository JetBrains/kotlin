class IncDec {
    operator fun inc(): Unit {}
}

fun foo(): IncDec {
    var x = IncDec()
    x = x++
    x++
    return x
}
