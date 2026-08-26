// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

var storage = ""
var receiverEvaluations = 0

context(c1: Int, c2: String)
var String.extProp: String
    get() = storage + this + c2 + c1
    set(value) {
        storage = value + this
    }

fun makeReceiver(): String {
    receiverEvaluations++
    return "r"
}

fun box(): String = context(1, "K") {
    class B {
        // Both the context arguments and the *extension* receiver are bound
        // (the other delegation tests only bind dispatch receivers).
        var y by makeReceiver()::extProp
    }
    val b = B()
    if (receiverEvaluations != 1) return@context "FAIL 0: $receiverEvaluations"
    b.y = "O"
    if (storage != "Or") return@context "FAIL 1: $storage"
    if (b.y != "OrrK1") return@context "FAIL 2: ${b.y}"
    // The bound extension receiver must be computed once and stored, not reevaluated per accessor call.
    if (receiverEvaluations != 1) return@context "FAIL 3: $receiverEvaluations"
    "OK"
}
