// !LANGUAGE: +ContextReceivers

class Ctx

fun Ctx.foo() {}

context(Ctx)
class A {
    fun bar(body: Ctx.() -> Unit) {
        foo()
        body()
    }
}

context(Ctx)
fun bar(body: Ctx.() -> Unit) {
    foo()
    body()
}
