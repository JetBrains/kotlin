// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.ExperimentalStdlibApi
package diagnostics

class Host1 {
    @JsSymbol("iterator")
    fun ok(hint: String): String = hint
}

<!JS_SYMBOL_ON_TOP_LEVEL_DECLARATION!>@JsSymbol("match")
fun topLevel() {}<!>

open class Base {
    open fun f() {}
}

class Derived : Base() {
    <!JS_SYMBOL_PROHIBITED_FOR_OVERRIDE!>@JsSymbol("replace")<!>
    override fun f() {}
}
