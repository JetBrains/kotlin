// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT




// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libCase1.boo

fun case1() {
    val y1 =<!UNRESOLVED_REFERENCE!>1::boo<!>
}

// FILE: LibCase1.kt
package libCase1

val Int.boo: String
    get() = "1"
fun Int.boo(): String =""

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2
import libCase2.*

fun case2() {
    val y1 =<!UNRESOLVED_REFERENCE!>1::boo<!>
}

// FILE: LibCase2.kt
package libCase2

val Int.boo: String
    get() = "1"
fun Int.boo(): String =""
