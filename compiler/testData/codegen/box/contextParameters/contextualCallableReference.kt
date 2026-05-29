// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: JVM_IR, NATIVE
// ^KT-86452

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
        val fnRef: () -> String = O::foo
        if (fnRef() != "OK") return "FAIL 1"

        val propRef = O::bar
        if (propRef() != "") return "FAIL 2"
        propRef.set("K")
        if (propRef() != "OK") return "FAIL 3"
    }

    return "OK"
}
