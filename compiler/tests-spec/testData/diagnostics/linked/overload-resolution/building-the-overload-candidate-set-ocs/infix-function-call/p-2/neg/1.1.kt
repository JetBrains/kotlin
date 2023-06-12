// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -EXTENSION_SHADOWED_BY_MEMBER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Implicitly imported extension callable without infix modifier
 */

// FILE: Extensions1.kt
package libPackage

class A() {
     fun foo(x: Int) = "member fun foo"
}

// FILE: Extensions2.kt
// TESTCASE NUMBER: 1, 2, 3, 4

package sentence3
import libPackage.A

fun A.foo(x: Int) = "pack scope extension fun foo"

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.A

class Case1() {
    fun A.foo(x: Int) = "local extension fun foo"

    fun case1() {
        val a = A()
        a <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
        A() <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
    }
}
// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.A

interface Case2 {
    fun A.foo(x: Int) = "local extension fun foo"

    fun case2() {
        val a = A()
        a <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
        A() <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
    }
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testPack
import libPackage.A

fun A.foo(x: Int) = "my package scope top level contains"


fun case3() {
    fun A.foo(x: Int) ="my local scope contains"

    val a = A()
    a <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
    A() <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
}

// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackNew
import libPackage.A

fun A.foo(x: Int) = "my package scope top level contains"


fun case4() {

    fun A.foo(x: Int) = "my local contains"

    fun subfun() {
        fun A.foo(x: Int) = "my local contains"
        val a = A()
        a <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
        A() <!INFIX_MODIFIER_REQUIRED!>foo<!> 1
    }
}
