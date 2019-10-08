fun foo() {
    require(true) { "foo" } /// *, L, λ

    require(true) { val a = 5 } /// *, L, λ

    require(true) { /// L
        val a = 5 /// L
    } /// L

    block { val a = 5 } /// *, L, λ

    block { /// L
        val a = 5 /// L
    } /// L

    inlineBlock { val a = 5} /// *, L, λ

    inlineBlock { /// L
        val a = 5 /// L
    } /// L

    inlineOnlyBlock { val a = 5 } /// *, L, λ

    inlineOnlyBlock { /// L
        val a = 5 /// L
    } /// L

    inlineOnlyBlock2 { val a = 5 } /// *, L, λ

    inlineOnlyBlock2 { /// L
        val a = 5 /// L
    } /// L
} /// L

private fun block(block: () -> Unit) { /// M
    block() /// L
} /// L

private inline fun inlineBlock(block: () -> Unit) { /// M
    block() /// L
} /// L

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
private inline fun inlineOnlyBlock(block: () -> Unit) {
    block()
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
private inline fun inlineOnlyBlock2(noinline block: () -> Unit) {
    block()
}