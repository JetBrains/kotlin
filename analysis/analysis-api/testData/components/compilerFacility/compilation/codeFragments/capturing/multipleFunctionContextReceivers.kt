// !LANGUAGE: +ContextReceivers
class Ctx1
class Ctx2

context(Ctx1, Ctx2)
fun useWithCtx1Ctx2() = 3

context(Ctx1, Ctx2)
fun foo() {
    <caret>val x = 1
}