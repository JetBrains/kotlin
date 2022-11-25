// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import testsCase1.Case1.Companion.foo

class Case1() {
    companion object {
        fun foo() : Case1 = TODO()
        fun foo(y: String, x: Any = "") : Case1 = TODO()
        fun foo( x: String, y: String) : Case1 = TODO()
    }

}
fun case1() {
    val y0: (String)-> Case1 = ::foo
    val y1: (String)-> Case1 = Case1.Companion::foo
    val y2: (String)-> Case1 = (Case1)::foo
}

fun case1_0() : (String)-> Case1 = ::foo
fun case1_1() : (String)-> Case1 = (Case1)::foo
fun case1_2(): (String)-> Case1 = Case1.Companion::foo
