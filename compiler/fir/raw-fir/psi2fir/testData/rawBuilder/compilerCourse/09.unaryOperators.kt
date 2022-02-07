fun test_1(x: String?) {
    val s = x!!
}

fun test_2_1() {
    var x = 10
    val y = x++
}

fun test_2_2() {
    var x = 10
    val y = ++x
}

fun test_3(x: Int) {
    val a = -x
    val b = +x
}
