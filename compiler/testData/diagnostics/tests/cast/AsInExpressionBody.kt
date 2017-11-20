fun <T> id(x: T): T = x

fun foo() = 1 as Any
fun bar() = id(1) as Any
fun baz() = (1 + 1) as Any

val functionLiteral1 = fun() = 1 as Any
val functionLiteral2 = fun() = id(1) as Any
val functionLiteral3 = fun() = (1 + 1) as Any

// TODO: this and more complex cases are not supported yet
fun baz(b: Boolean) = if (b) 1 <!USELESS_CAST!>as Any<!> else 42 <!USELESS_CAST!>as Any?<!>