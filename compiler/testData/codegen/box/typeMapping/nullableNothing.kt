
fun box(): String {
    // This used to be problematic because of an attempt to load jet/Nothing class
    val x = "" is Nothing?
    return "OK"
}