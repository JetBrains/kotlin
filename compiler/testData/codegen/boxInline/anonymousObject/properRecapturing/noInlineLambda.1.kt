import test.*

fun box(): String {
    var result = "fail"
    test { it -> result = it }
    return result
}
