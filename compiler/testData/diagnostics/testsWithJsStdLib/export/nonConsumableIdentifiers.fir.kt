// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("delete")!>@JsExport
fun delete() {}<!>

<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("instanceof")!>@JsExport
val instanceof = 4<!>

<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("eval")!>@JsExport
class eval<!>

@JsExport
@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("await")!>"await"<!>)
fun foo() {}

@JsExport
@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("this")!>"this"<!>)
val bar = 4

@JsExport
@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER("super")!>"super"<!>)
class Baz

@JsExport
@JsName("default")
class DefDef

@JsExport
class Test {
    fun instanceof() {}

    @JsName("eval")
    fun test() {}
}

@JsExport
object NaN

@JsExport
enum class Nums {
    Infinity,
    undefined
}
