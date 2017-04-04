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

private fun assertContents(ctx: CoroutineContext, vararg elements: CoroutineContext.Element) {
    val set = ctx.fold(setOf<CoroutineContext>()) { a, b -> a + b }
    assertEquals(listOf(*elements), set.toList())
    for (elem in elements)
        assertTrue(ctx[elem.key] == elem)
}

fun box() {
    val ctx1 = CtxA(1) + CtxB(2)
    val ctx2 = CtxB(3) + CtxC(4)
    val ctx = ctx1 + ctx2
    assertContents(ctx, CtxA(1), CtxB(3), CtxC(4))
    assertEquals("[CtxA(i=1), CtxB(i=3), CtxC(i=4)]", ctx.toString())
    assertEquals(CtxA(1), ctx[CtxA])
    assertEquals(CtxB(3), ctx[CtxB])
    assertEquals(CtxC(4), ctx[CtxC])
}
