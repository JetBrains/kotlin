fun test1(): Int {
    return 1.inlineMethod()
}

fun box(): String {
    val result = test1()
    if (result != 2) return "test1: ${result}"

    return "OK"
}