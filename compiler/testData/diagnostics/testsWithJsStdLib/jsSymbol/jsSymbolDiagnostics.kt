// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// OPT_IN: kotlin.js.ExperimentalJsExport
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

class WithPrimaryCtor <!JS_SYMBOL_ON_PRIMARY_CONSTRUCTOR_PROHIBITED!>@JsSymbol("hasInstance")<!> constructor()
