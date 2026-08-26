// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

var sink = ""
var receiverEvaluations = 0

class A(val tag: String) {
    context(c1: Int, c2: String)
    var prop: String
        get() = tag + c2 + c1
        set(value) {
            sink = tag + value + c1
        }
}

fun makeA(): A {
    receiverEvaluations++
    return A("a")
}

fun box(): String = context(1, "K") {
    class B {
        var y by makeA()::prop
    }
    val b = B()
    if (receiverEvaluations != 1) return@context "FAIL 0: $receiverEvaluations"
    b.y = "O"
    if (sink != "aO1") return@context "FAIL 1: $sink"
    if (b.y != "aK1") return@context "FAIL 2: ${b.y}"
    // The non-trivial bound receiver must be computed once and stored, not reevaluated per accessor call.
    if (receiverEvaluations != 1) return@context "FAIL 3: $receiverEvaluations"
    "OK"
}
