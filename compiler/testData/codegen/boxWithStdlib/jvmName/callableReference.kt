@JvmName("bar")
fun foo() = "foo"

fun box(): String {
    val f = (::foo)()
    if (f != "foo") return "Fail: $f"

    return "OK"
}