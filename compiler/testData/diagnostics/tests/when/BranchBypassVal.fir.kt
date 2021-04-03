// !WITH_NEW_INFERENCE
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 1
 * expressions, when-expression -> paragraph 5 -> sentence 1
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 6
 */
class A

fun test(a: Any): String {
    val q: String? = null

    when (a) {
        is A -> q!!
    }
    // When is not exhaustive
    return <!RETURN_TYPE_MISMATCH!>q<!>
}
