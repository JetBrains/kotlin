fun box(): String {
    var v = "FAIL"
    val x = object : JavaClass({-> v = "OK"}) {}
    x.run()
    return v
}
