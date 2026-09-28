// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

fun box(): String = context("K") {
    class A(val tag: String) {
        context(c: String)
        val prop: String
            get() = tag + c

        val y by A::prop
    }
    val result = A("a").y
    if (result != "aK") return@context "FAIL: $result"
    "OK"
}
