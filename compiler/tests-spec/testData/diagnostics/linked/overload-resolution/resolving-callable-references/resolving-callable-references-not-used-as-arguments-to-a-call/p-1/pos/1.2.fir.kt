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
 * NUMBER: 2
 * DESCRIPTION: the case of a call with a callable reference as a not parameter
 */


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libCase1.*
import kotlin.text.format

fun case1() {
    val y2 : () ->String =(String)::format
}

// FILE: LibCase1.kt
package libCase1

val String.Companion.format: String
    get() = "1"


// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2
import libCase2.*
import kotlin.text.format

fun case2() {
    //
    val x = "".format::invoke
    //
    val y = String.format::invoke
}

fun String.invoke(format: String, vararg args: Any?): String = "" //(2)

val String.format: String
    get() = "1"


val String.Companion.format: String
    get() = "1"


// FILE: LibCase2.kt
package libCase2


val String.Companion.format: String
    get() = "1"

fun String.invoke(format: String, vararg args: Any?): String = ""


val String.format: String
    get() = "1"



// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testsCase3
import libCase3.format
import kotlin.text.*

fun case3() {
    val y1 =(String)::format
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty1<kotlin.String, kotlin.Int>")!>y1<!>

    val y2 =""::format
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty0<kotlin.Int>")!>y2<!>
}

// FILE: LibCase3.kt
package libCase3

val String.Companion.format: Unit
    get() = TODO()

val String.format: Int
    get() = TODO()
