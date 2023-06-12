// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -USELESS_IS_CHECK
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: type-system, type-contexts-and-scopes, inner-and-nested-type-contexts -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: type context for a nested type declaration of a parent type declaration does not include the type parameters of PD
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1

class Case1<AT>(val x: AT) {

    inner class C() {
        fun case1a(x: Any) {
            if (x is <!CANNOT_CHECK_FOR_ERASED!>AT<!>) {
                ""
            }
        }

        fun case1b(x: Any) {
            when (x) {
                is <!CANNOT_CHECK_FOR_ERASED!>AT<!> -> println("at")
            }
        }
    }
}