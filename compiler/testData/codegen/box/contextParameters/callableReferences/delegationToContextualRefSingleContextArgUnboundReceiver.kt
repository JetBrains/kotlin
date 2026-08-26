// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

fun box(): String = context("K") {
    class A(val tag: String) {
        context(c: String)
        val prop: String
            get() = tag + c

        // The single bound value of the reference is the context argument, not the receiver: the receiver of `prop`
        // stays unbound and is provided by the delegated property's own receiver.
        val y by A::prop
    }
    val result = A("a").y
    if (result != "aK") return@context "FAIL: $result"
    "OK"
}
