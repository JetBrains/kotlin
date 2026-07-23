// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

var storage = ""

context(c: Ctx)
var prop: String
    get() = storage + c.tag
    set(value) {
        storage = value + c.tag
    }

class Ctx(val tag: String) {
    // The enclosing class instance is the bound context argument of the reference, so the accessors of `y`
    // must remap `this` captured in the delegate initializer to their own dispatch receiver.
    var y by ::prop
}

fun box(): String {
    val a = Ctx("A")
    val b = Ctx("B")
    a.y = "O"
    if (storage != "OA") return "FAIL 1: $storage"
    if (a.y != "OAA") return "FAIL 2: ${a.y}"
    if (b.y != "OAB") return "FAIL 3: ${b.y}"
    b.y = "X"
    if (storage != "XB") return "FAIL 4: $storage"
    if (b.y != "XBB") return "FAIL 5: ${b.y}"
    return "OK"
}
