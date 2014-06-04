import test.*

fun simple(): String {
    val k = "K"
    return simpleFun(lambda = {it + "O"}) + simpleFun("K", {k + it})
}

fun simpleR(): String {
    val k = "K"
    return simpleFunR({it + "O"}) + simpleFunR({k + it}, "K")
}

fun box(): String {

    var result = simple()
    if (result != "OOKK") return "fail1: ${result}"

    result = simpleR()
    if (result != "OOKK") return "fail2: ${result}"

    return "OK"
}