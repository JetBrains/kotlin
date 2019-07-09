fun foo(first: Int, second: Double, third: Boolean, fourth: String) {}

fun test() {
    foo(1, 2.0, true, "")
    foo(1, 2.0, true, fourth = "!")
    foo(1, 2.0, fourth = "???", third = false)
    foo(1, second = 3.14, third = false, fourth = "!?")
    foo(third = false, second = 2.71, fourth = "?!", first = 0)

    foo()
    foo(0.0, false, 0, "")
    foo(1, 2.0, third = true, "")
    foo(second = 0.0, first = 0, fourth = "")
    foo(first = 0.0, second = 0, third = "", fourth = false)
    foo(first = 0, second = 0.0, third = false, fourth = "", first = 1)
    foo(0, 0.0, false, foth = "")
}