import test.*

fun testCompilation(): String {
    emptyFun()
    emptyFun("K")

    return "OK"
}

fun simple(): String {
    return simpleFun() + simpleFun("K")
}

fun box(): String {
    var result = testCompilation()
    if (result != "OK") return "fail1: ${result}"

    result = simple()
    if (result != "OK") return "fail2: ${result}"

    return "OK"
}