// MODULE: lib
// FILE: A.kt
// VERSION: 1

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


// FILE: B.kt
// VERSION: 2

fun foo_a(a1: String) = a1
fun foo_b(b1: String = "foo_b default") = b1
fun foo_c(vararg c1: String) = c1.joinToString(", ")

// swap order
fun foo_d(b: String, a: String) = b + a
fun foo_e(b: String = "foo_e default a", a: String = "foo_e default b") = b + a
fun foo_f(vararg b: String, a: String) = b.joinToString(", ") + a

class X() {
    fun bar_a(a1: String) = a1
    fun bar_b(b1: String = "foo_b default") = b1
    fun bar_c(vararg c1: String) = c1.joinToString(", ")

    fun bar_d(b: String, a: String) = b + a
    fun bar_e(b: String = "foo_e default a", a: String = "foo_e default b") = b + a
    fun bar_f(vararg b: String, a: String) = b.joinToString(", ") + a
}

class qux_a(a1: String) {
    val x = a1
}
class qux_b(b1: String = "foo_b default") {
    val x = b1
}
class qux_c(vararg c1: String) {
    val x = c1.joinToString(", ")
}

// swap order
class qux_d(b: String, a: String) {
    val x = b + a
}

class qux_e(b: String = "foo_e default a", a: String = "foo_e default b") {
    val x = b + a
}
class qux_f(vararg b: String, a: String) {
    val x= b.joinToString(", ") + a
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String {
  val x = X()
  return when {
    foo_a("first") != "first" -> "fail 1"
    foo_b() != "foo_b default" -> "fail 2"
    foo_c("first", "second") != "first, second" -> "fail 3"
    foo_d("first", "second") != "firstsecond" -> "fail 4"
    foo_d(a = "first", b = "second") != "firstsecond" -> "fail 5"
    foo_e() != "foo_e default afoo_e default b" -> "fail 6"
    foo_e(a = "first", b = "second") != "firstsecond" -> "fail 7"
    foo_f("second", "third", b = "first") != "second, thirdfirst" -> "fail 8"

    x.bar_a("first") != "first" -> "fail 11"
    x.bar_b() != "foo_b default" -> "fail 12"
    x.bar_c("first", "second") != "first, second" -> "fail 13"
    x.bar_d("first", "second") != "firstsecond" -> "fail 14"
    x.bar_d(a = "first", b = "second") != "firstsecond" -> "fail 15"
    x.bar_e() != "foo_e default afoo_e default b" -> "fail 16"
    x.bar_e(a = "first", b = "second") != "firstsecond" -> "fail 17"
    x.bar_f("second", "third", b = "first") != "second, thirdfirst" -> "fail 18"

    qux_a("first").x != "first" -> "fail 21"
    qux_b().x != "foo_b default" -> "fail 22"
    qux_c("first", "second").x != "first, second" -> "fail 23"
    qux_d("first", "second").x != "firstsecond" -> "fail 24"
    qux_d(a = "first", b = "second").x != "firstsecond" -> "fail 25"
    qux_e().x != "foo_e default afoo_e default b" -> "fail 26"
    qux_e(a = "first", b = "second").x != "firstsecond" -> "fail 27"
    qux_f("second", "third", b = "first").x != "second, thirdfirst" -> "fail 28"

    else -> "OK"
  }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

