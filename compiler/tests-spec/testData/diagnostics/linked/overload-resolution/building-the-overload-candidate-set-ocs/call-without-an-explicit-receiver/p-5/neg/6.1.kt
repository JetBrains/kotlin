// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-401
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 6
 * NUMBER: 1
 * DESCRIPTION:
 */

// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase1
import libCase1.*
import kotlin.text.*

fun case1() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>Regex<!>("")
}

// FILE: Lib1.kt
package libCase1
fun Regex(pattern: String) {}


// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-39073
 */
package testPackCase2
import libCase2.a.*
import libCase2.b.*
import kotlin.text.*


fun case2() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>Regex<!>("")
}

// FILE: Lib2.kt
package libCase2.a
fun Regex(pattern: String) {}

// FILE: Lib3.kt
package libCase2.b
fun Regex(pattern: String) {}


// FILE: TestCase4.kt
/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39157
 */
package testPackCase4
import libCase4.a.*
import libCase4.b.*
import kotlin.text.*

fun case4() {
    <!DEBUG_INFO_CALL("fqName: libCase4.a.Regex; typeCall: function")!>Regex("")<!>
}

// FILE: Lib4.kt
package libCase4.a
fun Regex(pattern: String) {}

// FILE: Lib5.kt
package libCase4.b
class Regex(pattern: String) {}



// FILE: TestCase5.kt
/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase5
import libCase5.a.*
import libCase5.b.*

fun case5() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>Regex<!>("")
}

// FILE: Lib6.kt
package libCase5.a
fun Regex(pattern: String) {}

// FILE: Lib7.kt
package libCase5.b
class Regex(pattern: String) {}

// FILE: TestCase6.kt
/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase6
import libCase6.a.*
import libCase6.b.*

fun case6() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>MyRegex<!>("")
}

// FILE: Lib8.kt
package libCase6.a
fun MyRegex(pattern: String) {}

// FILE: Lib9.kt
package libCase6.b
class MyRegex(pattern: String) {}



