// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT



// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libCase1.*

fun case1() {
    val y1 =<!UNRESOLVED_REFERENCE!>(A)::boo<!>
}

// FILE: LibCase1.kt
package libCase1

class A{
    companion object{}
}
val A.Companion.boo: String
    get() = "1"
fun A.Companion.boo(): String =""


// FILE: TestCase2.kt
// TESTCASE NUMBER: 1
package testsCase2
import libCase2.A
import libCase2.boo

fun case2() {
    val y1 =<!UNRESOLVED_REFERENCE!>(A)::boo<!>
}

// FILE: LibCase2.kt
package libCase2

class A{
    companion object{}
}
val A.Companion.boo: String
    get() = "1"
fun A.Companion.boo(): String =""
