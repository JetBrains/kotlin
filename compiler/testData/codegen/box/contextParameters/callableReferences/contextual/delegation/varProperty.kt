// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

var storage = ""

context(c1: Int, c2: String)
var contextualizedProp: String
    get() = storage + c2 + c1
    set(value) {
        storage = value
    }

fun box(): String = context(1, "K") {
    class B {
        var y by ::contextualizedProp
    }
    val b = B()
    b.y = "O"
    if (storage != "O") return@context "FAIL 1: $storage"
    if (b.y != "OK1") return@context "FAIL 2: ${b.y}"
    "OK"
}
