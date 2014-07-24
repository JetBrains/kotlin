fun test1(): String {
    val inlineX = My()
    var d = "";
    inlineX.doWork({(z: String) -> d = z; z})
    return d
}

fun test2(): Int {
    val inlineX = My()
    return inlineX.perform({(z: My) -> 11})
}

fun box(): String {
    if (test1() != "OK") return "test1: ${test1()}"
    if (test2() != 11) return "test1: ${test2()}"

    return "OK"
}