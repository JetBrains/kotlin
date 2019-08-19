// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 64
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-16373
 */
class Case1<T> {
    fun getT(): T = TODO()
    fun getTN(): T? = null

    fun get(): T {
        var x = getTN()
        if (x == null) {
            x = getT()
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("T?")!>x<!>
        return <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
    }
}

// TESTCASE NUMBER: 2
class Case2 {
    fun getInt(): Int = 10
    fun getIntN(): Int? = null

    fun get(): Int {
        var x = getIntN()
        if (x == null) {
            x = getInt()
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
        return <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-16373
 */
class Case3<T> {
    fun getT(): T = TODO()
    fun getTN(): T? = null

    fun get(): T {
        var x = getTN()
        x = x ?: getT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T?")!>x<!>
        return <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
    }
}

// TESTCASE NUMBER: 4
class Case4 {
    fun getInt(): Int = 10
    fun getIntN(): Int? = null

    fun get(): Int {
        var x = getIntN()
        x = x ?: getInt()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
        return <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}

// TESTCASE NUMBER: 5
class Case5<T> {
    fun getT(): T = TODO()
    fun getTN(): T? = null

    fun get(): T {
        var x = getTN()
        x = if (x == null) getT() else <!DEBUG_INFO_SMARTCAST!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T?")!>x<!>
        return <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}

// TESTCASE NUMBER: 6
class Case6 {
    fun getInt(): Int = 10
    fun getIntN(): Int? = null

    fun get(): Int {
        var x = getIntN()
        x = if (x == null) getInt() else <!DEBUG_INFO_SMARTCAST!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
        return <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}