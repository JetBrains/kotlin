// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T : Any> jso(): T =
    js("({})")

fun <T : Any> jso(
    block: T.() -> Unit,
): T = jso<T>().apply(block)

object FooBar {
    fun buildNodes() = jso<dynamic> {
        this["code_block"] = ParseRuleBuilder.create(getAttrs = ::readCodeBlockAttrs)
    }

    private fun readCodeBlockAttrs() {
    }
}

object ParseRuleBuilder {
    fun create(
        getAttrs: Any? = null,
    ) {
    }
}