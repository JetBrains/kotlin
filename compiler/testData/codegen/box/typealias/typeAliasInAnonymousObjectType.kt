open class Foo<T>(val x: T)

typealias FooStr = Foo<String>

val test = object : FooStr("OK") {}

fun box() = test.x