import test.*

fun box() : String {
    return if (call("123", String::length) == 3) "OK" else "fail"
}
