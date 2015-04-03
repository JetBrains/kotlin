fun test1(): Int {
    val inlineX = Inline()
    return inlineX.foo({ z: Int -> "" + z}, 25, fun String.(): Int = this.length())
}

fun box(): String {
    if (test1() != 2) return "test1: ${test1()}"

    return "OK"
}
