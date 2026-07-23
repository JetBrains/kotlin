// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

var sideEffects = ""

object O {
    var storage = ""

    @JvmStatic
    context(c: Int)
    var prop: String
        get() = storage + c
        set(value) {
            storage = value
        }
}

fun makeO(): O {
    sideEffects += "makeO;"
    return O
}

fun box(): String = context(1) {
    // The reference binds both a context argument and a receiver with side effects;
    // the @JvmStatic-in-object rewrite must keep the former and normalize the latter.
    val ref = makeO()::prop
    if (sideEffects != "makeO;") return@context "FAIL 0: $sideEffects"
    ref.set("O")
    if (O.storage != "O") return@context "FAIL 1: ${O.storage}"
    if (ref.get() != "O1") return@context "FAIL 2: ${ref.get()}"
    "OK"
}
