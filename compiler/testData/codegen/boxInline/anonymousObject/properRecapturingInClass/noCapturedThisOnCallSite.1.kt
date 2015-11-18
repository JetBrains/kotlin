import test.*

fun box(): String {
    var result = "fail"
    B("O", "fail").test { it -> result = it }
    return result
}
