import kotlin.test.*

import kotlin.coroutines.experimental.*

data class CtxA(val i: Int) : AbstractCoroutineContextElement(CtxA) {
    companion object Key : CoroutineContext.Key<CtxA>
}

data class CtxB(val i: Int) : AbstractCoroutineContextElement(CtxB) {
    companion object Key : CoroutineContext.Key<CtxB>
}

data class CtxC(val i: Int) : AbstractCoroutineContextElement(CtxC) {
    companion object Key : CoroutineContext.Key<CtxC>
}

object Disp1 : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = continuation
    override fun toString(): String = "Disp1"
}

object Disp2 : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = continuation
    override fun toString(): String = "Disp2"
}

private fun assertContents(ctx: CoroutineContext, vararg elements: CoroutineContext.Element) {
    val set = ctx.fold(setOf<CoroutineContext>()) { a, b -> a + b }
    assertEquals(listOf(*elements), set.toList())
    for (elem in elements)
        assertTrue(ctx[elem.key] == elem)
}

fun box() {
    var ctx: CoroutineContext = EmptyCoroutineContext
    assertContents(ctx)
    ctx += CtxA(1)
    assertContents(ctx, CtxA(1))
    ctx += Disp1
    assertContents(ctx, CtxA(1), Disp1)
    ctx += CtxA(2)
    assertContents(ctx, CtxA(2), Disp1)
    ctx += CtxB(3)
    assertContents(ctx, CtxA(2), CtxB(3), Disp1)
    ctx += Disp2
    assertContents(ctx, CtxA(2), CtxB(3), Disp2)
    ctx += (CtxB(4) + CtxC(5))
    assertContents(ctx, CtxA(2), CtxB(4), CtxC(5), Disp2)
}
