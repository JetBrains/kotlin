fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj[{ v = "O" }] = { v += "K" }
    return v
}
