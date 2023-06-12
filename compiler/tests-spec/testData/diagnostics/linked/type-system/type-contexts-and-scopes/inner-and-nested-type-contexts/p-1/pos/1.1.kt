// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -USELESS_IS_CHECK -UNCHECKED_CAST
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: type-system, type-contexts-and-scopes, inner-and-nested-type-contexts -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: type context for a nested type declaration of a parent type declaration does not include the type parameters of PD
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1

class Case1<AT>(val x: AT) {

    inner class B(val y: AT) {
        fun case1a(k: AT) {
            k checkType { check<AT>() }
            with(k) {
                this checkType { check<AT>() }
            }

        }

        fun case1b() {
            val k: AT = x
            k checkType { check<AT>() }

            when (k) {
                is AT -> {
                    k checkType { check<AT>() }
                }
            }
        }

        fun case1c() {
            val k: AT = x!!

            if (k is AT) {
                k checkType { check<AT>() }
            }
        }
    }

    inner class D() {
        fun boo(x: Any): AT = TODO()
        fun case1() {
            boo("") checkType { check<AT>() }
        }
    }
}