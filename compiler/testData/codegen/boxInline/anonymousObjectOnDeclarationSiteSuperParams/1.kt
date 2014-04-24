import test.*

fun test1(): String {
    val o = "O"

    val result = doWork ({o}, {"K"}, "11")

    return result.getO() + result.getK() + result.param
}

fun test2() : String {
    //same names as in object
    val o1 = "O"
    val k1 = "K"
    val param = "11"
    val result = doWorkInConstructor ({o1}, {k1}, {param})

    return result.getO() + result.getK() + result.param
}

fun box() : String {
    val result1 = test1();
    if (result1 != "OK11") return "fail1 $result1"

    val result2 = test2();
    if (result2 != "OK11") return "fail2 $result2"

    return "OK"
}

