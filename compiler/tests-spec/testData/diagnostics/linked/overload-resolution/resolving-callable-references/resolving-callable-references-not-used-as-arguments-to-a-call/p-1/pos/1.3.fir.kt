// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-409
 * MAIN LINK: overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 2
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 3
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 4
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 6
 * NUMBER: 3
 * DESCRIPTION: the case of a call with a callable reference as a not parameter
 */


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

class Case() {
    fun case(v: V) {
        // InitializertTypeCheckerMismatch bug
        val va: () -> String = (V)::a

        val vb: () -> String = (V)::b

        val va1: () -> String = v::a
        val vb1: () -> String = (V)::b

    }

    val V.Companion.b: String // (3)
        get() = "1"

}

val V.a: String
    get() = "1"

val V.Companion.a: String
    get() = "1"


class V {
    companion object {
        const val b: String = "1"
    }
}
