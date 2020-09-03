// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 * PRIMARY LINKS: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 10
 * expressions, try-expressions -> paragraph 2 -> sentence 1
 * expressions, try-expressions -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: notNull smartcast & direct assignment smartcast
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1() {
    var c: Any? = SomeEnum1.FOO

    if (c != null) {
        <!DEBUG_INFO_SMARTCAST!>c<!> checkType { check<Any>() }
        c = SomeEnum1.BAR + try {
            SomeEnum1.valueOf("X")
        } catch (e: Exception) {
            throw Exception()
        }
        <!DEBUG_INFO_SMARTCAST!>c<!> checkType { check<SomeEnum1>() }

    }
}

enum class SomeEnum1() {
    FOO, BAR
}

operator fun <T> SomeEnum1?.plus(x: T): T = TODO()
