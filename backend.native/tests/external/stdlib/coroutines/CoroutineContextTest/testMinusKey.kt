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
    var ctx: CoroutineContext = CtxA(1) + CtxB(2) + CtxC(3)
    assertContents(ctx, CtxA(1), CtxB(2), CtxC(3))
    assertEquals("[CtxA(i=1), CtxB(i=2), CtxC(i=3)]", ctx.toString())

    ctx = ctx.minusKey(CtxA)
    assertContents(ctx, CtxB(2), CtxC(3))
    assertEquals("[CtxB(i=2), CtxC(i=3)]", ctx.toString())
    assertEquals(null, ctx[CtxA])
    assertEquals(CtxB(2), ctx[CtxB])
    assertEquals(CtxC(3), ctx[CtxC])

    ctx = ctx.minusKey(CtxC)
    assertContents(ctx, CtxB(2))
    assertEquals("CtxB(i=2)", ctx.toString())
    assertEquals(null, ctx[CtxA])
    assertEquals(CtxB(2), ctx[CtxB])
    assertEquals(null, ctx[CtxC])

    ctx = ctx.minusKey(CtxC)
    assertContents(ctx, CtxB(2))
    assertEquals("CtxB(i=2)", ctx.toString())
    assertEquals(null, ctx[CtxA])
    assertEquals(CtxB(2), ctx[CtxB])
    assertEquals(null, ctx[CtxC])

    ctx = ctx.minusKey(CtxB)
    assertContents(ctx)
    assertEquals("EmptyCoroutineContext", ctx.toString())
    assertEquals(null, ctx[CtxA])
    assertEquals(null, ctx[CtxB])
    assertEquals(null, ctx[CtxC])

    assertEquals(EmptyCoroutineContext, ctx)
}
