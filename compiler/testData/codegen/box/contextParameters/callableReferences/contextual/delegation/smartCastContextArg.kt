// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

var storage = ""

context(c1: Int, c2: String)
var prop: String
    get() = storage + c2 + c1
    set(value) {
        storage = value
    }

context(c: Any)
fun test(): String {
    if (c !is String) return "FAIL 0: $c"
    return context(1) {
        class B {
            var y by ::prop
        }
        val b = B()
        b.y = "O"
        if (storage != "O") return@context "FAIL 1: $storage"
        if (b.y != "OK1") return@context "FAIL 2: ${b.y}"
        "OK"
    }
}

fun box(): String = context<Any, String>("K") { test() }
