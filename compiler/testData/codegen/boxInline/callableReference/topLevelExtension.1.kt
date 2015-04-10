import test.*

fun box() : String {
    return if (call(10, Int::calc) == 100) "OK" else "fail"
}

fun Int.calc(p: Int) : Int {
    return p * this
}