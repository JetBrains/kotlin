class C(val x: String)

typealias Alias = C

fun box(): String = Alias("OK").x
