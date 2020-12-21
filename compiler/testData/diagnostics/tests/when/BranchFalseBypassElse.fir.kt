/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 1
 * expressions, when-expression -> paragraph 5 -> sentence 1
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 8
 */
class A

fun test(a: Any): String {
    var q: String?

    when (a) {
        is A -> q = "1"
        else -> q = "2"
    }
    // When is not exhaustive
    return q
}
