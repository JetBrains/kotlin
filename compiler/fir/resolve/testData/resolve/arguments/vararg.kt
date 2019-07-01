fun foo(x: Int, vararg y: String) {}
fun bar(x: Int, vararg y: String, z: Boolean) {}

fun test() {
    foo(1)
    foo(1, "")
    foo(1, "my", "yours")
    foo(1, *arrayOf("my", "yours"))

    foo("")
    foo(1, 2)

    bar(1, z = true, y = *arrayOf("my", "yours"))

    bar(0, z = false, y = "", y = "other")
    bar(0, "", true)
}
