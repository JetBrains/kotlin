// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-448
 * MAIN LINK: overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 2 -> sentence 2
 * PRIMARY LINKS: overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 4 -> sentence 3
 * SECONDARY LINKS: overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: explicit receiver with lambda
 */

// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 */
package testPackCase1

import LibPackCase1.a.boo
import LibPackCase1.b.*

fun case1 (b: B?){
    b.boo({ <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH!>C()<!> })
    b.boo({1})
}

class B {
    private fun boo(c: () -> C) {}
}
class C

// FILE: LibCase11.kt
package LibPackCase1.b
import LibPackCase1.a.*
import testPackCase1.B
import testPackCase1.C

fun B?.boo( c: ()->C) {} //(2)


// FILE: LibCase12.kt
package LibPackCase1.a
import testPackCase1.B

fun B?.boo( c: ()->Int) { //(1)
}
