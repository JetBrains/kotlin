// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 0, 1, 2, 3, 4
// FILE: Extensions1.kt
package libPackage

class A() {
    infix fun foo(x: ()->Int) = "member fun foo"
}

// FILE: Extensions2.kt
// TESTCASE NUMBER: 0

package sentence3
import libPackage.A

infix fun A.foo(x: ()->Int) = "pack scope extension fun foo"

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package sentence3
import libPackage.A

class Case1() {
    infix fun A.foo(x: ()->Int) = "local extension fun foo"

    fun case1() {
        val a = A()
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo {1}<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo {1}<!>
    }
}
// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package sentence3
import libPackage.A

interface Case2 {
    infix fun A.foo(x: ()->Int) = "local extension fun foo"

    fun case2() {
        val a = A()
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo {1}<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo {1}<!>
    }
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testPack
import libPackage.A

infix fun A.foo(x: ()->Int) = "my package scope top level trim"


fun case3() {
    infix fun A.foo(x: ()->Int) = "my local scope trim"

    val a = A()
    <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo {1}<!>
    <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo {1}<!>
}

// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackNew
import libPackage.A

infix fun A.foo(x: ()->Int) = "my package scope top level trim"


fun case4() {

    infix fun A.foo(x: ()->Int) = "my local trim"

    fun subfun() {
        infix fun A.foo(x: ()->Int) = "my local trim"
        val a = A()
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>a foo {1}<!>
        <!DEBUG_INFO_CALL("fqName: libPackage.A.foo; typeCall: infix function")!>A() foo {1}<!>
    }
}

// FILE: TestCase5.kt
// TESTCASE NUMBER: 5
package testPackNew

class A() {
    fun foo(i: ()->Int) {}
    infix fun A.foo(i: ()->Int) {}

    fun bar(a: A) {
        //todo: add info if function is infix one
        <!DEBUG_INFO_CALL("fqName: testPackNew.A.foo; typeCall: infix extension function")!>a foo {1}<!>
    }

    fun buz(a: A) {
        fun foo(i: ()->Int) {}
        //todo: add info if function is infix one
        <!DEBUG_INFO_CALL("fqName: testPackNew.A.foo; typeCall: infix extension function")!>a foo {1}<!>
    }

    fun boo(a: A) {
        infix fun A.foo(i: ()->Int) {}
        <!DEBUG_INFO_CALL("fqName: testPackNew.A.boo.foo; typeCall: infix extension function")!>a foo {1}<!>
    }
}
