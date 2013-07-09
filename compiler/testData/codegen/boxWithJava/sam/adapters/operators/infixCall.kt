fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj doSomething { v = "OK" }
    return v
}
