// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

var storage = ""

context(c1: Int, c2: String)
var contextualizedProp: String
    get() = storage + c2 + c1
    set(value) {
        storage = value
    }

context(c1: Int, c2: String)
val readOnlyProp: String
    get() = c2 + c1

fun box(): String = context(1, "K") {
    val r by ::readOnlyProp
    var y by ::contextualizedProp
    y = "O"
    if (storage != "O") return@context "FAIL 1: $storage"
    if (y != "OK1") return@context "FAIL 2: $y"
    if (r != "K1") return@context "FAIL 3: $r"
    "OK"
}
