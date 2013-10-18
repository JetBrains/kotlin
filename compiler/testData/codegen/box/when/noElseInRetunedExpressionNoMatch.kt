var r = "OK"

fun foo(x: Int) {
    return when (x) {
        1 -> { r = "Fail" }
    }
}
fun box(): String {
    foo(0)
    return r
}