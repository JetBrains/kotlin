// !WITH_NEW_INFERENCE
class A

fun test(a: Any) {
    var q: String? = null

    when (a) {
        is A -> q = "1"
    }
    // When is not exhaustive
    return <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>q<!>
}
