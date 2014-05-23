import test.*

fun test1(): String {
    val o = "O"

    val result = doWork ({o}, {"K"}, "GOOD")

    return result.getO() + result.getK() + result.getParam()
}

fun test2() : String {
    //same names as in object
    val o1 = "O"
    val k1 = "K"

    val result = doWorkInConstructor ({o1}, {k1}, "GOOD")

    return result.getO() + result.getK() + result.getParam()
}

fun box() : String {
    val result1 = test1();
    if (result1 != "OKGOOD") return "fail1 $result1"

    val result2 = test2();
    if (result2 != "OKGOOD") return "fail2 $result2"

    return "OK"
}

