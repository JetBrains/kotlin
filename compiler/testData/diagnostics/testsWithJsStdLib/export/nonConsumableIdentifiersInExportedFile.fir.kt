// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES
@file:JsExport

package foo

<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("delete")!>fun delete() {}<!>

<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("instanceof")!>val instanceof = 4<!>

<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("eval")!>class eval<!>

@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("await")!>"await"<!>)
fun foo() {}

@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("this")!>"this"<!>)
val bar = 4

@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("super")!>"super"<!>)
class Baz

class Test {
    fun instanceof() {}

    @JsName("eval")
    fun test() {}
}

object NaN

enum class Nums {
    Infinity,
    undefined
}
