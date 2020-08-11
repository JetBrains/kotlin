// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 8
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 9
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 10
 * NUMBER: 1
 * DESCRIPTION: A companion object of a classifier type named foo at its declaration site
 */

// FILE: Case1.kt
package testPackage

// TESTCASE NUMBER: 1

class Case1() {
    companion object foo {
        operator fun invoke() {}

        fun f() {
            <!DEBUG_INFO_CALL("fqName: testPackage.Case1.foo.invoke; typeCall: variable&invoke")!>foo()<!>
            foo.<!DEBUG_INFO_CALL("fqName: testPackage.Case1.foo.invoke; typeCall: operator function")!>invoke()<!>
        }
    }
}


// FILE: LibCase2.kt
package libPackage
fun zoo() {}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackage
import libPackage.zoo as foo

class Case2() {
    companion object foo{
        operator fun invoke() {}

        fun f() {
            <!DEBUG_INFO_CALL("fqName: testPackage.Case2.foo.invoke; typeCall: variable&invoke")!>foo()<!>
        }
    }

    fun f() {
        <!DEBUG_INFO_CALL("fqName: testPackage.Case2.foo.invoke; typeCall: variable&invoke")!>foo()<!>
    }
}
