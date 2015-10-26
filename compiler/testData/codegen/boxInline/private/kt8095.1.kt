import test.*

fun box(): String {
    var r = "fail"
    C("OK").g { r = it }
    return r
}