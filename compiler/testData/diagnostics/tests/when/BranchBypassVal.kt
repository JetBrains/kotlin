// !WITH_NEW_INFERENCE
class A

fun test(a: Any): String {
    val q: String? = null

    when (a) {
        is A -> q!!
    }
    // When is not exhaustive
    return <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>q<!>
}
