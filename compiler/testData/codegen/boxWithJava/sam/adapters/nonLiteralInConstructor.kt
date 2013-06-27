fun box(): String {
    var v = "FAIL"
    val f = { v = "OK" }
    JavaClass(f).run()
    return v
}