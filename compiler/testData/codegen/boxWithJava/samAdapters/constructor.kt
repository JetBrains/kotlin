fun box(): String {
    var v = "FAIL"
    JavaClass { v = "OK" }.run()
    return v
}