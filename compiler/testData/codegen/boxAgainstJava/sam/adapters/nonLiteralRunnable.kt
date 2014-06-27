fun box(): String {
    var v = "FAIL"
    val f = { v = "OK" }
    JavaClass.run(f)
    return v
}