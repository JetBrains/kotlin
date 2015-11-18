import test.*

fun box(): String {
    var result = "fail"
    B("O", "K").test { it -> result = it }
    return result
}
