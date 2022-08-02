fun box(): String {
    val a = ""

    val c = fun(): String {
        if (a != "") return "Fail"
        return "OK"
    }.invoke()

    return c
}
