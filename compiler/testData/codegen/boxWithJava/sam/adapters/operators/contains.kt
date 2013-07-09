fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    { v = "O" } in obj
    { v += "K" } !in obj
    return v
}
