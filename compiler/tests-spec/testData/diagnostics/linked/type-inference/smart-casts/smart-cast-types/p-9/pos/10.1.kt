// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 10
 * PRIMARY LINKS: expressions, try-expressions -> paragraph 2 -> sentence 1
 * expressions, try-expressions -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: nullable variable smartcast from direct assignment of try-catch expression with enums
 * HELPERS: checkType
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41607
 */
enum class SomeEnum1 {
    FOO, BAR
}

fun case1() {

    var enumVar: SomeEnum1? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    enumVar = try {
        val x = SomeEnum1.valueOf("X")
        x
    } catch (e: Exception) {
        throw RuntimeException()
    }
    enumVar checkType { <!NONE_APPLICABLE!>check<!><SomeEnum1>() } //KT-41607
    enumVar checkType { check<SomeEnum1?>() }
}

// TESTCASE NUMBER: 2
enum class SomeEnum2 {
    FOO, BAR
}

fun case2() {
    var enumVar: Any? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    enumVar = try {
        SomeEnum2.valueOf("X")
    } catch (e: Exception) {
        throw RuntimeException()
    }
    <!DEBUG_INFO_SMARTCAST!>enumVar<!> checkType { check<SomeEnum2>() }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41607
 */
enum class SomeEnum3 {
    FOO, BAR
}

fun case3() {

    var enumVar: SomeEnum3? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    enumVar = try {
        SomeEnum3.valueOf("X")
    } catch (e: Exception) {
        SomeEnum3.valueOf("FOO")
    }
    enumVar checkType { <!NONE_APPLICABLE!>check<!><SomeEnum3>() } //KT-41607
    enumVar checkType { check<SomeEnum3?>() }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41607
 */
enum class SomeEnum4 {
    FOO, BAR
}

fun case4() {

    var enumVar: SomeEnum4? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    enumVar = try {
        SomeEnum4.valueOf("X")
    } catch (e: Exception) {
        SomeEnum4.FOO
    }
    enumVar checkType { <!NONE_APPLICABLE!>check<!><SomeEnum4>() } //KT-41607
    enumVar checkType { check<SomeEnum4?>() }
}

// TESTCASE NUMBER: 5
fun case5() {
    var x: Any? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
    x = try {
        SomeEnum5.valueOf("X")
    } catch (e: Exception) {
        SomeEnum5.get("")
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> checkType { check<SomeEnum5>() }
}

enum class SomeEnum5 {
    FOO, BAR;

    companion object {
        fun get(s: String) = try {
            valueOf(s)
        } catch (e: Exception) {
            valueOf("Foo")
        }
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41607
 */
enum class SomeEnum6 {
    FOO, BAR
}

fun case6() {

    var enumVar: SomeEnum6? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    enumVar = try {
        SomeEnum6.valueOf("X")
    } catch (e: Exception) {
        throw RuntimeException()
    }
    enumVar checkType { <!NONE_APPLICABLE!>check<!><SomeEnum6>() } //KT-41607
    enumVar checkType { check<SomeEnum6?>() }
}

// TESTCASE NUMBER: 7

enum class SomeEnum7(val flag: Boolean) {
    FOO(false), BAR(true)

}

fun case7() {

    var enumVar: Any? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    enumVar = try {
        val x = SomeEnum7.valueOf("X")
        if (x.flag) x else SomeEnum7.BAR
    } catch (e: Exception) {
        throw RuntimeException()
    }
    <!DEBUG_INFO_SMARTCAST!>enumVar<!> checkType { check<SomeEnum7>() }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41607
 */
enum class SomeEnum8(val flag: Boolean) {
    FOO(false), BAR(true)

}

fun case8() {

    var enumVar: SomeEnum8? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    enumVar = try {
        val x = SomeEnum8.valueOf("X")
        if (x.flag) x else SomeEnum8.BAR
    } catch (e: Exception) {
        throw RuntimeException()
    }
    enumVar checkType { <!NONE_APPLICABLE!>check<!><SomeEnum8>() } //KT-41607
    enumVar checkType { check<SomeEnum8?>() }
}