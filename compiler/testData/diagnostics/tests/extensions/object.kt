object O

fun Any.foo() = 42
val Any?.bar = 239

val x = O.foo() + O.bar
