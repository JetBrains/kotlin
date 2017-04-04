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
    var ctx: CoroutineContext = EmptyCoroutineContext
    assertContents(ctx)
    assertEquals("EmptyCoroutineContext", ctx.toString())

    ctx += CtxA(1)
    assertContents(ctx, CtxA(1))
    assertEquals("CtxA(i=1)", ctx.toString())
    assertEquals(CtxA(1), ctx[CtxA])
    assertEquals(null, ctx[CtxB])
    assertEquals(null, ctx[CtxC])

    ctx += CtxB(2)
    assertContents(ctx, CtxA(1), CtxB(2))
    assertEquals("[CtxA(i=1), CtxB(i=2)]", ctx.toString())
    assertEquals(CtxA(1), ctx[CtxA])
    assertEquals(CtxB(2), ctx[CtxB])
    assertEquals(null, ctx[CtxC])

    ctx += CtxC(3)
    assertContents(ctx, CtxA(1), CtxB(2), CtxC(3))
    assertEquals("[CtxA(i=1), CtxB(i=2), CtxC(i=3)]", ctx.toString())
    assertEquals(CtxA(1), ctx[CtxA])
    assertEquals(CtxB(2), ctx[CtxB])
    assertEquals(CtxC(3), ctx[CtxC])

    ctx += CtxB(4)
    assertContents(ctx, CtxA(1), CtxC(3), CtxB(4))
    assertEquals("[CtxA(i=1), CtxC(i=3), CtxB(i=4)]", ctx.toString())
    assertEquals(CtxA(1), ctx[CtxA])
    assertEquals(CtxB(4), ctx[CtxB])
    assertEquals(CtxC(3), ctx[CtxC])

    ctx += CtxA(5)
    assertContents(ctx, CtxC(3), CtxB(4), CtxA(5))
    assertEquals("[CtxC(i=3), CtxB(i=4), CtxA(i=5)]", ctx.toString())
    assertEquals(CtxA(5), ctx[CtxA])
    assertEquals(CtxB(4), ctx[CtxB])
    assertEquals(CtxC(3), ctx[CtxC])
}
