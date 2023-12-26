// JVM_ABI_K1_K2_DIFF: KT-63864
open class Foo<T>(val x: T)

typealias FooStr = Foo<String>

val test = object : FooStr("OK") {}

fun box() = test.x