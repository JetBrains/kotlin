object O

fun Any.foo() = 42
val Any?.bar: Int get() = 239

val x = O.foo() + O.bar
