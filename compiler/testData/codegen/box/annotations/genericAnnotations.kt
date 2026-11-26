value class SomeValue(val v: Int)

fun lolkek(): Int {
    var mmm = SomeValue(4)
    keklol {
        mmm = SomeValue(42)
    }
    return mmm.v
}

fun keklol(x: () -> Unit) {
    x()
}


fun box(): String {
    lolkek()
    return "OK"
}
