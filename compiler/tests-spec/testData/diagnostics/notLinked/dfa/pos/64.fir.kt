// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

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
        return <!RETURN_TYPE_MISMATCH!>x<!>
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.equals(10)
        return x
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T")!>x<!>
        return x
    }
}

// TESTCASE NUMBER: 4
class Case4 {
    fun getInt(): Int = 10
    fun getIntN(): Int? = null

    fun get(): Int {
        var x = getIntN()
        x = x ?: getInt()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.equals(10)
        return x
    }
}

// TESTCASE NUMBER: 5
class Case5<T> {
    fun getT(): T = TODO()
    fun getTN(): T? = null

    fun get(): T {
        var x = getTN()
        x = if (x == null) getT() else x
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T")!>x<!>
        return x
    }
}

// TESTCASE NUMBER: 6
class Case6 {
    fun getInt(): Int = 10
    fun getIntN(): Int? = null

    fun get(): Int {
        var x = getIntN()
        x = if (x == null) getInt() else x
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.equals(10)
        return x
    }
}
