fun box(): String {
    var v = "FAIL"
    val f = { v = "O" }
    JavaClass.run(f, { v += "K" })
    return v
}