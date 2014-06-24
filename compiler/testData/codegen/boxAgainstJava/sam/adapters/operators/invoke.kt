fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj({ v = "O" })
    obj { v += "K" }
    return v
}
