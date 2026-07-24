// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// WITH_REFLECT
// IGNORE_BACKEND: JVM_IR
// ^KT-86452, KT-87390

object O {
    context(s: String, c: Char)
    fun foo(default: Int = 0) = s + c
}

var _x = ""

context(s: String)
var O.bar: String
    get() = _x
    set(v) {
        _x = s + v
    }

fun box(): String {
    context("O", 'K') {
        val fnRefType = O::foo.returnType.toString()
        if (fnRefType != "kotlin.String") return "FAIL 1: $fnRefType != kotlin.String"

        val propRefType = O::bar.returnType.toString()
        if (propRefType != "kotlin.String") return "FAIL 2: $propRefType != kotlin.String"
    }

    return "OK"
}
