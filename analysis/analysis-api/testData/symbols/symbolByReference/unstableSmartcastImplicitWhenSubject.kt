// DO_NOT_CHECK_SYMBOL_RESTORE_K1

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