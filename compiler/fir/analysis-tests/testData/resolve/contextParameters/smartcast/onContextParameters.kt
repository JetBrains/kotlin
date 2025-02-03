// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

interface Ctx
class CtxImpl: Ctx {
    fun method() {}
}

context(ctx: CtxImpl)
fun function() {}

context(ctx: Ctx)
fun test() {
    if (ctx is CtxImpl) {
        ctx.method()
    }
}

context(ctx: Ctx)
fun test2() {
    if (ctx is CtxImpl) {
        function()
    }
    else {
        <!NO_CONTEXT_ARGUMENT!>function<!>()
    }
}