import test.*

class Z {}

fun test1() : Int {
    val input = Z()
    return input.use<Z, Int>{
        100
    }
}

fun test2() : Int {
    val x = 1000
    return use2() + x
}


fun box(): String {
    if (test1() != 100) return "test1: ${test1()}"
    if (test2() != 1100) return "test1: ${test2()}"

    return "OK"
}
