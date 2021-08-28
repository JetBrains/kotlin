// TARGET_BACKEND: JVM_IR

fun interface Foo1 : () -> String
fun interface Foo2 : Foo1

fun test2(foo: Foo2, expected: String) {
    if (foo.toString() != expected) throw AssertionError("expected $expected but found $foo")
}

fun box(): String {
    val foo1 = object : Foo1 {
        override fun invoke() = "123"
        override fun toString() = "foo1"
    }

    test2(foo1, "foo1")
    return "OK"
}