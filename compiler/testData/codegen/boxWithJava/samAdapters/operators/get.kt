fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj[{ v = "OK" }]
    return v
}
