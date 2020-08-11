// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 10
 * NUMBER: 1
 * DESCRIPTION: An enum entry named foo at its declaration site prio
 */

// FILE: LibCase1.kt
// TESTCASE NUMBER: 1, 2

package libPackage

fun foo() {}


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1

package testPackage
import libPackage.foo as foo

class Case1() {
    enum class A {
        zoo, boo;

        fun f() {
            <!DEBUG_INFO_CALL("fqName: libPackage.foo; typeCall: function")!>foo()<!>
        }

        operator fun invoke() {}
    }
}


// FILE: TestCase2.kt
// TESTCASE NUMBER: 2

package testPackage
import libPackage.foo as zoo

class Case2() {
    companion object zoo {
        fun f() {
            <!DEBUG_INFO_CALL("fqName: libPackage.foo; typeCall: function")!>zoo()<!>
        }
    }

    fun f() {
        <!DEBUG_INFO_CALL("fqName: libPackage.foo; typeCall: function")!>zoo()<!>
    }
}
