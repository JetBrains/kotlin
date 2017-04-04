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

fun box() {
    val ctx1 = CtxA(1) + CtxB(2) + CtxC(3)
    val ctx2 = CtxB(2) + CtxC(3) + CtxA(1) // same
    val ctx3 = CtxC(3) + CtxA(1) + CtxB(2) // same
    val ctx4 = CtxA(1) + CtxB(2) + CtxC(4) // different
    assertEquals(ctx1, ctx2)
    assertEquals(ctx1, ctx3)
    assertEquals(ctx2, ctx3)
    assertNotEquals(ctx1, ctx4)
    assertNotEquals(ctx2, ctx4)
    assertNotEquals(ctx3, ctx4)
}
