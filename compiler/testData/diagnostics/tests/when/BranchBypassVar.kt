// !WITH_NEW_INFERENCE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-155, test type: neg):
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 1
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 8
 */
class A

fun test(a: Any) {
    var q: String? = null

    when (a) {
        is A -> q = "1"
    }
    // When is not exhaustive
    return <!TYPE_MISMATCH!>q<!>
}
