// FIR_IDENTICAL
// ISSUE: KT-61937
// !LANGUAGE: +ContextReceivers

class Ctx

context(Ctx)
fun Ctx.foo(): String = "NOK"

context(Ctx)
fun bar(foo: Ctx.() -> String ): String {
    return foo()
}

fun box(): String = with (Ctx()) {
    bar { "OK" }
}
