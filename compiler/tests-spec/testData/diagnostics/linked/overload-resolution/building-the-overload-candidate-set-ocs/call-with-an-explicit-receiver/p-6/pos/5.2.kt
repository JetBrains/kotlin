// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 11 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: set of star-imported extension callables
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libCase1.*
import kotlin.text.format

fun Case1() {
    //
    val x0  = "".format.invoke("")
    val x1  = "".format.<!DEBUG_INFO_CALL("fqName: testsCase1.invoke; typeCall: extension function")!>invoke("")<!>

    //
    val y0  = String.format.invoke("")
    val y1  = String.format.<!DEBUG_INFO_CALL("fqName: testsCase1.invoke; typeCall: extension function")!>invoke("")<!>

}

fun String.invoke(format: String, vararg args: Any?): String = "" //(2)

val String.format: String
    get() = "1"


val String.Companion.format: String
    get() = "1"


// FILE: LibCase1.kt
package libCase1


val String.Companion.format: String
    get() = "1"

fun String.invoke(format: String, vararg args: Any?): String = ""


val String.format: String
    get() = "1"
