fun foo(first: Int, second: Double = 3.14, third: Boolean = false) {}
fun bar(first: Int, second: Double = 2.71, third: Boolean, fourth: String = "") {}
fun baz(x: Int, vararg y: String, z: Boolean = false) {}

fun test() {
    foo(1)
    foo(1, 2.0)
    foo(1, 2.0, true)
    foo(1, third = true)

    foo()
    foo(0, 0.0, false, "")

    bar(1, third = true)
    bar(1, 2.0, true)
    bar(1, 2.0, true, "my")

    bar(1, true)

    baz(1)
    baz(1, "my", "yours")
    baz(1, z = true)

    baz(0, "", false)
}

