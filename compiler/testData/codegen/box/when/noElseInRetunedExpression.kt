var r = "Fail 0"

fun foo(x: Int) {
    return when (x) {
        1 -> { r = "OK" }
    }
}
fun box(): String {
    foo(1)
    return r
}