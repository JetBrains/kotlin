import test.*

fun box(): String {
    var r = "fail"
    A().foo { r = it }
    return r
}