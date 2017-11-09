// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Ctx
class CtxImpl : Ctx {
    fun doJob(a: Int) {}
    fun doJob(s: String) {}
}

open class Test(open val ctx: Ctx) {
    fun test() {
        when (ctx) {
            is CtxImpl -> <!SMARTCAST_IMPOSSIBLE!>ctx<!>.doJob(2)
        }
    }
}