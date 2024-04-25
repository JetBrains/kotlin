// FULL_JDK
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT



// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

class Case() {
    fun case(v: V) {
        // InitializerTypeMismatchChecker bug
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
