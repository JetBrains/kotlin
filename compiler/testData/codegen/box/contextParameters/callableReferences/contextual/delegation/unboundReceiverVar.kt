// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

var storage = ""

fun box(): String = context(1, "K") {
    class A(val tag: String) {
        context(c1: Int, c2: String)
        var prop: String
            get() = storage + tag + c2 + c1
            set(value) {
                storage = value + tag
            }

        var y by A::prop
    }
    val a = A("a")
    a.y = "O"
    if (storage != "Oa") return@context "FAIL 1: $storage"
    if (a.y != "OaaK1") return@context "FAIL 2: ${a.y}"
    "OK"
}
