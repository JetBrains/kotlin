fun box(): String {
    var r = "FAIL"
    val sub = Sub()
    sub.safeInvoke(null)
    sub.safeInvoke { r = "OK" }
    return r
}
