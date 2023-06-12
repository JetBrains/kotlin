// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -EXTENSION_SHADOWED_BY_MEMBER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 4
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Local extension infix extension callables
 */

// TESTCASE NUMBER: 0, 1, 2, 3, 4
// FILE: Extensions1.kt
package libPackage

class A() {
    infix fun foo(x: Int) = "member fun foo"
}

// FILE: Extensions2.kt
// TESTCASE NUMBER: 0

package sentence3
import libPackage.A
infix fun A.foo(x: Int) = "pack scope extension fun foo"

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.A

class Case1() {
    infix fun A.foo(x: Int) = "local extension fun foo"

    fun case1() {
        val a = A()
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo 1<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo 1<!>
    }
}
// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.A

interface Case2 {
    infix fun A.foo(x: Int) = "local extension fun foo"

    fun case2() {
        val a = A()
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo 1<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo 1<!>
    }
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testPack
import libPackage.A

infix fun A.foo(x: Int) = "my package scope top level contains"


fun case3() {
    infix fun A.foo(x: Int) = "my local scope contains"

    val a = A()
    <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo 1<!>
    <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo 1<!>
}

// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackNew
import libPackage.A

infix fun A.foo(x: Int) = "my package scope top level contains"


fun case4() {

    infix fun A.foo(x: Int) = "my local contains"

    fun subfun() {
        infix fun A.foo(x: Int) = "my local contains"
        val a = A()
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo 1<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo 1<!>
    }
}

// FILE: TestCase5.kt
// TESTCASE NUMBER: 5
package testPackNew

class A() {
    fun foo(i: Int) {}
    infix fun A.foo(i: Int) {}

    fun bar(a: A) {
        //todo: add info if function is infix one
       <!DEBUG_INFO_CALL("fqName: testPackNew.A.foo; typeCall: infix extension function")!>a foo 1<!>
    }

    fun buz(a: A){
        fun foo(i: Int) {}
        //todo: add info if function is infix one
        <!DEBUG_INFO_CALL("fqName: testPackNew.A.foo; typeCall: infix extension function")!>a foo 1<!>
    }

    fun boo(a: A){
        infix fun A.foo(i: Int) {}
        <!DEBUG_INFO_CALL("fqName: testPackNew.A.boo.foo; typeCall: infix extension function")!>a foo 1<!>
    }
}
