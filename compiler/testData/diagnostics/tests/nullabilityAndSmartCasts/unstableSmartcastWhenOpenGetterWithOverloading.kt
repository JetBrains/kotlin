// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

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

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, isExpression,
primaryConstructor, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
