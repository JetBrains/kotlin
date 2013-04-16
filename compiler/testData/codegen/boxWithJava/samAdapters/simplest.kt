fun box(): String {
    var v = "FAIL"
    JavaClass.run { v = "OK" }
    return v
}