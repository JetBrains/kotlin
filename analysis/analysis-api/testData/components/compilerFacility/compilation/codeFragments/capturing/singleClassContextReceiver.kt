// LANGUAGE: +ContextReceivers
class Ctx1

context(Ctx1)
fun useWithCtx1() = 3

context(Ctx1)
class Test {
    fun foo() {
        <caret>val x = 1
    }
}
