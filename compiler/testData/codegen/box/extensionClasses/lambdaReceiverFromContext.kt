// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// !LANGUAGE: +ContextReceivers

class Ctx(val value: Int)

fun Ctx.foo() = value + 4

context(Ctx)
class A {
    fun bar(body: Ctx.() -> Int): Int {
        return foo() *
                body()
    }
}

fun box(): String {
    val res = with(Ctx(3)) {
        A().bar { this.value * 2 }
    }
    return if (res == 42) "OK" else "NOK: $res"
}
