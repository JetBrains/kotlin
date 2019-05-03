// !WITH_NEW_INFERENCE
class A

fun test(a: Any): String {
    val q: String? = null

    when (a) {
        is A -> q!!
    }
    // When is not exhaustive
    return <!TYPE_MISMATCH!>q<!>
}
