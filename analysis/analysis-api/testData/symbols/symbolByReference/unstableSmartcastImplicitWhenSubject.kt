// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

interface Ctx
class CtxImpl : Ctx {
    fun doJob(a: Int) {}
    fun doJob(s: String) {}
}

open class Test(open val ctx: Ctx) {
    fun test() {
        when (ctx) {
            is CtxImpl -> <caret>ctx.doJob(2)
        }
    }
}