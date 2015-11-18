import test.*

fun box(): String {
    var result = ""
    B("O", "K").test { it -> result += it }
    return if (result == "startOOKK") "OK" else "fail: $result"
}
