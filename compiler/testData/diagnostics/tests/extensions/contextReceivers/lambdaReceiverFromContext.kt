// !LANGUAGE: +ContextReceivers

class Ctx

fun Ctx.foo() {}

context(Ctx)
class A {
    fun bar(body: Ctx.() -> Unit) {
        foo()
        body<!NO_VALUE_FOR_PARAMETER!>()<!>
    }
}

context(Ctx)
fun bar(body: Ctx.() -> Unit) {
    foo()
    body<!NO_VALUE_FOR_PARAMETER!>()<!>
}
