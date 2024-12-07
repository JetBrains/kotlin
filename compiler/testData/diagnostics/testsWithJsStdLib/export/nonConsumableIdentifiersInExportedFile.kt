// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTICS_MESSAGES
@file:JsExport

package foo

fun <!NON_CONSUMABLE_EXPORTED_IDENTIFIER!>delete<!>() {}

val <!NON_CONSUMABLE_EXPORTED_IDENTIFIER!>instanceof<!> = 4

class <!NON_CONSUMABLE_EXPORTED_IDENTIFIER!>eval<!>

@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER!>"await"<!>)
fun foo() {}

@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER!>"this"<!>)
val bar = 4

@JsName(<!NON_CONSUMABLE_EXPORTED_IDENTIFIER!>"super"<!>)
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