// !DIAGNOSTICS: -UNUSED_PARAMETER

fun fun1() {}
fun fun1(x: Int) {}

val ref1 = <!UNRESOLVED_REFERENCE!>::fun1<!>

fun fun2(vararg x: Int) {}
fun fun2(x: Int) {}

val ref2 = <!UNRESOLVED_REFERENCE!>::fun2<!>

fun fun3(x0: Int, vararg xs: Int) {}
fun fun3(x0: String, vararg xs: String) {}

val ref3 = <!UNRESOLVED_REFERENCE!>::fun3<!>
