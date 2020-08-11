// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 9
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 10
 * NUMBER: 1
 * DESCRIPTION: An enum entry named foo at its declaration site prio
 */

// FILE: Case1.kt
package testPackage

// TESTCASE NUMBER: 1

class Case1() {
    enum class A {
        foo, boo;

        fun f() {
            <!DEBUG_INFO_CALL("fqName: testPackage.Case1.A.invoke; typeCall: variable&invoke")!>foo()<!>
            foo.<!DEBUG_INFO_CALL("fqName: testPackage.Case1.A.invoke; typeCall: operator function")!>invoke()<!>
        }

        operator fun invoke() {}
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
    enum class A {
        foo, boo, zoo;

        fun f() {
            <!DEBUG_INFO_CALL("fqName: testPackage.Case2.A.invoke; typeCall: variable&invoke")!>foo()<!>
        }

        operator fun invoke() {}
    }
}
