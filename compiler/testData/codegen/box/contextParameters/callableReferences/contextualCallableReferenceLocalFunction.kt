// LANGUAGE: +ContextParameters +CallableReferencesToContextual

fun box(): String {
    var captured = "O"

    context(s: String)
    fun local(suffix: String) = captured + s + suffix

    return context("_") {
        val ref: (String) -> String = ::local
        val r = ref("K")
        if (r != "O_K") "FAIL: $r" else "OK"
    }
}
