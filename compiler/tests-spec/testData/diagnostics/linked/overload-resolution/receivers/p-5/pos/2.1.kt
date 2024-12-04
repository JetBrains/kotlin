// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * MAIN LINK: overload-resolution, receivers -> paragraph 5 -> sentence 2
 * PRIMARY LINKS: overload-resolution, receivers -> paragraph 5 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: The implicit this receiver has higher priority than phantom static implicit this
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testsCase1

open class A {
    operator fun invoke() = print("invoke")
}

open class B {
    operator fun invoke() = print("invoke")
}

enum class Super_2 {
    V1, V2;

    val values = A()

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase1.A.invoke; typeCall: variable&invoke")!>values()<!>
    }

    enum class NestedWithCompanion {
        V1;

        val values = B()

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase1.B.invoke; typeCall: variable&invoke")!>values()<!>
        }
    }

    enum class Nested {
        V1;

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase1.Super_2.Nested.values; typeCall: function")!>values()<!>
        }
    }
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testsCase2

open class A {
    operator fun invoke(value: String) = print("invoke $value")
}
open class B {
    operator fun invoke(value: String) = print("invoke $value")
}

enum class Super_2 {
    V1, V2;

    val valueOf = A()

    private fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase2.A.invoke; typeCall: variable&invoke")!>valueOf("")<!>
    }

    enum class NestedWithCompanion {
        V1;

        val valueOf = B()

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase2.B.invoke; typeCall: variable&invoke")!>valueOf("")<!>
        }
    }

    enum class Nested {
        V1;

        private fun case() {
            <!DEBUG_INFO_CALL("fqName: testsCase2.Super_2.Nested.valueOf; typeCall: function")!>valueOf("")<!>
        }
    }
}
