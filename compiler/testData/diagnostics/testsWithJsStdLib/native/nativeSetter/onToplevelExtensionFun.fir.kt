// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

@nativeSetter
fun Int.set(a: String, v: Int) {}

@nativeSetter
fun Int.set2(a: Number, v: String?): Any? = null

@nativeSetter
fun Int.set3(a: Double, v: String) = "OK"

@nativeSetter
fun Int.set4(a: Double, v: String): Any = 1

@nativeSetter
fun Int.set5(a: Double, v: String): CharSequence = "OK"

@nativeSetter
fun Int.set6(a: Double, v: String): Number = 1

@nativeSetter
fun Any.foo(a: String = "0.0", v: String = "str") = "OK"

@nativeSetter
fun Int.set(a: <!UNRESOLVED_REFERENCE!>A<!>): Int? = 1

@nativeSetter
fun Int.set2(): String? = "OK"

@nativeSetter
fun Int.set3(a: Any, b: Int, c: Any?) {}
