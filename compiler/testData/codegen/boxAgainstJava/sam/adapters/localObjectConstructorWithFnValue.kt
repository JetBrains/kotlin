fun box(): String {
    var v = "FAIL"
    val f = {-> v = "OK"}
    val x = object : JavaClass(f) {}
    x.run()
    return v
}
