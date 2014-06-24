fun box(): String {
    val obj = JavaClass()

    var v1 = "FAIL"
    obj[{ v1 = "O" }, { v1 += "K" }]
    if (v1 != "OK") return "get: $v1"

    var v2 = "FAIL"
    obj[{ v2 = "" }, { v2 += "O" }] = { v2 += "K" }
    if (v2 != "OK") return "set: $v2"

    return "OK"
}
