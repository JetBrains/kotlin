// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-409
 * MAIN LINK: overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 2
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 3
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 4
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 2 -> sentence 5
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 5 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: the case of a call with a callable reference as a not parameter
 */



// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1
import libCase1.boo

fun case1() {
    val y1 =1::<!OVERLOAD_RESOLUTION_AMBIGUITY!>boo<!>
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
    val y1 =1::<!OVERLOAD_RESOLUTION_AMBIGUITY!>boo<!>
}

// FILE: LibCase2.kt
package libCase2

val Int.boo: String
    get() = "1"
fun Int.boo(): String =""
