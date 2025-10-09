fun foo_a(a: String) = a
fun foo_b(b: String = "foo_b default") = b
fun foo_c(vararg c: String) = c.joinToString(", ")

fun foo_d(a: String, b: String) = a + b
fun foo_e(a: String = "foo_e default a", b: String = "foo_e default b") = a + b
fun foo_f(vararg a: String, b: String) = a.joinToString(", ") + b

class X() {
    fun bar_a(a: String) = a
    fun bar_b(b: String = "foo_b default") = b
    fun bar_c(vararg c: String) = c.joinToString(", ")

    fun bar_d(a: String, b: String) = a + b
    fun bar_e(a: String = "foo_e default a", b: String = "foo_e default b") = a + b
    fun bar_f(vararg a: String, b: String) = a.joinToString(", ") + b
}

class qux_a(a: String) {
    val x = a
}
class qux_b(b: String = "foo_b default") {
    val x = b
}
class qux_c(vararg c: String) {
    val x = c.joinToString(", ")
}

// swap order
class qux_d(a: String, b: String) {
    val x = a + b
}

class qux_e(a: String = "foo_e default a", b: String = "foo_e default b") {
    val x = a + b
}
class qux_f(vararg a: String, b: String) {
    val x= a.joinToString(", ") + b
}


