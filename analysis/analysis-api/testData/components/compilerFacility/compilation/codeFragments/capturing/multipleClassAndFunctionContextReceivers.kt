// LANGUAGE: +ContextReceivers
class Ctx1
class Ctx2
class Ctx3
class Ctx4

context(Ctx1, Ctx2, Ctx3, Ctx4)
fun useWithCtx1Ctx2Ctx3Ctx4() = 3

context(Ctx1, Ctx2)
class Test {
    context(Ctx3, Ctx4)
    fun foo() {
        <caret>val x = 1
    }
}
