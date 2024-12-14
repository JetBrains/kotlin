// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun interface Foo : () -> Int

fun id(foo: Foo): Any = foo

fun box(): String {
    val p1 = object : Foo {
        override fun invoke(): Int = 42
        override fun toString(): String = "OK"
    }
    val p2 = id(p1)

    if (p1 !== p2) return "Fail identity equals"
    if (p1.toString() != p2.toString()) return "Fail toString"

    return p2.toString()
}
