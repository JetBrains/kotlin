// TARGET_BACKEND: JVM
// WITH_REFLECT

fun interface Foo1 : () -> String
fun interface Foo2 : Foo1
interface Foo3 : () -> String
fun interface Foo4 {
    fun invoke(): String
}

fun test1(foo: Foo1, expected: String) {
    if (foo.toString() != expected) throw AssertionError("expected $expected but found $foo")
}

fun test2(foo: Foo2, expected: String) {
    if (foo.toString() != expected) throw AssertionError("expected $expected but found $foo")
}

fun testFunctionReference() {
    val expected = "fun simpleFunction(): kotlin.String"
    if (Foo4(::simpleFunction).toString() != expected)
        throw AssertionError("expected $expected but found ${Foo4(::simpleFunction)}")
}

fun regression(foo: Foo1) {
    if (foo.invoke() != "123") throw AssertionError("expected \"123\" but found ${foo.invoke()}")
}

fun simpleFunction(): String = "123"

fun box(): String {
    val foo1 = object : Foo1 {
        override fun invoke() = "123"
        override fun toString() = "foo1"
    }

    val foo2 = object : Foo2 {
        override fun invoke() = "123"
        override fun toString() = "foo2"
    }

    val foo3 = object : Foo3 {
        override fun invoke(): String = "123"
        override fun toString(): String = "foo3"
    }

    val bar: () -> String = { "123" }

    test1(foo1, "foo1")
    test1(foo2, "foo2")
    test2(foo2, "foo2")
    test2(foo1, "foo1")
    test1(foo3, "foo3")
    test2(foo3, "foo3")
    testFunctionReference()

    regression(bar)
    return "OK"
}