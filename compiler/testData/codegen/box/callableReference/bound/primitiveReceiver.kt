fun Boolean.foo() = 1
fun Byte.foo() = 2
fun Short.foo() = 3
fun Int.foo() = 4
fun Long.foo() = 5
fun Char.foo() = 6
fun Float.foo() = 7
fun Double.foo() = 8

fun testRef(name: String, f: () -> Int, expected: Int) {
    val actual = f()
    if (actual != expected) throw AssertionError("$name: $actual != $expected")
}

fun box(): String {
    testRef("Boolean", true::foo, 1)
    testRef("Byte", 1.toByte()::foo, 2)
    testRef("Short", 1.toShort()::foo, 3)
    testRef("Int", 1::foo, 4)
    testRef("Long", 1L::foo, 5)
    testRef("Char", '1'::foo, 6)
    testRef("Float", 1.0F::foo, 7)
    testRef("Double", 1.0::foo, 8)

    return "OK"
}