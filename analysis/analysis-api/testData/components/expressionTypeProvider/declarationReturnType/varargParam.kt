class Foo(vararg constructorValues: String)

fun test(vararg values: String) {}

fun test(vararg primitiveValues: Int) {}

fun <T> generic(vararg ts: T) {}

class Bar(vararg val props: Long)

fun functions(vararg fs: () -> Unit) {}
