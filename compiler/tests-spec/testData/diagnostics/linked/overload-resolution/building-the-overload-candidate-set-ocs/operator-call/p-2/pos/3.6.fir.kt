// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: LibCase1.kt
// TESTCASE NUMBER: 1
package libPackage

/*public*/ private inline operator fun CharSequence.contains(regex: Regex): Boolean = regex.containsMatchIn(this)


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1
import libPackage.*

class Case() {

    fun foo() {
        "".<!DEBUG_INFO_CALL("fqName: kotlin.text.contains; typeCall: inline operator extension function")!>contains(Regex(""))<!>
    }
}
