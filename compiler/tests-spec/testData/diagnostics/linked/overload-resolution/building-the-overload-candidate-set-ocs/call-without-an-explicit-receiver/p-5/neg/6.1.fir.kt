// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
package testPackCase1
import libCase1.*
import kotlin.text.*

fun case1() {
    <!AMBIGUITY!>Regex<!>("")
}

// FILE: Lib.kt
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
    <!AMBIGUITY!>Regex<!>("")
}

// FILE: Lib.kt
package libCase2.a
fun Regex(pattern: String) {}

// FILE: Lib.kt
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
    <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!><!AMBIGUITY!>Regex<!>("")<!>
}

// FILE: Lib.kt
package libCase4.a
fun Regex(pattern: String) {}

// FILE: Lib.kt
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
    <!AMBIGUITY!>Regex<!>("")
}

// FILE: Lib.kt
package libCase5.a
fun Regex(pattern: String) {}

// FILE: Lib.kt
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
    <!AMBIGUITY!>MyRegex<!>("")
}

// FILE: Lib.kt
package libCase6.a
fun MyRegex(pattern: String) {}

// FILE: Lib.kt
package libCase6.b
class MyRegex(pattern: String) {}
