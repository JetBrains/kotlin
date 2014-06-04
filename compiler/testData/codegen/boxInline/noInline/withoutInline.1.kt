fun test1(): Int {
    val inlineX = Inline()
    var p = {(l : Int) -> l};
    return inlineX.calc(p, 25)
}

fun box(): String {
    if (test1() != 25) return "test1: ${test1()}"

    return "OK"
}