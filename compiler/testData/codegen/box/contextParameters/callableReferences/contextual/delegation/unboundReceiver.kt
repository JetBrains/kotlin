// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

fun box(): String = context(1, "K") {
    class A(val tag: String) {
        context(c1: Int, c2: String)
        val prop: String
            get() = tag + c2 + c1

        val y by A::prop
    }
    val result = A("a").y
    if (result != "aK1") return@context "FAIL: $result"
    "OK"
}
