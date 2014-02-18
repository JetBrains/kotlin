fun test1(): Int {
    var s = 0;
    2.times2 {
        s++
    }
    return s;
}

fun box(): String {
    if (test1() != 2) return "test1: ${test1()}"

    return "OK"
}