// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: type-system, type-contexts-and-scopes, inner-and-nested-type-contexts -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: type context for a nested type declaration of a parent type declaration does not include the type parameters of PD
 */

// TESTCASE NUMBER: 1
class Case1<AT>(val x: AT) {

    class B(val y: <!UNRESOLVED_REFERENCE!>AT<!>) {
        fun case1() {
            val k: <!UNRESOLVED_REFERENCE!>AT<!>
        }
    }

    class C() {
        fun case1(x: Any) {
            when (x) {
                is <!UNRESOLVED_REFERENCE!>AT<!> -> println("at")
                else -> println("else")
            }
        }
    }

    class D() {
        fun case1(x: Any) : <!UNRESOLVED_REFERENCE!>AT<!> = TODO()
    }
}