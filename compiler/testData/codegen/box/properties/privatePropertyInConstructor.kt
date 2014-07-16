class A(
        private val x: String,
        private var y: Double
) {
    fun foo() {
        val r = {
            if (x != "abc") throw AssertionError("$x")
            y = 0.0
            if (y != 0.0) throw AssertionError("$y")
        }
        r()
    }
}

fun box(): String {
    A("abc", 3.14).foo()
    return "OK"
}
