// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
open class BaseCase1(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case1a() : BaseCase1(1, 1) {
    val a1: Int = 1
    var a2: Int = 1
    val b1: Int = 1
    var b2: Int = 1
}

open class Case1b() : BaseCase1(1, 1) {
    val a1 = 1
    var a2 = 1
    val b1 = 1
    var b2 = 1
}

// TESTCASE NUMBER: 2
open class BaseCase2(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case2() : BaseCase2(1, 1) {
    val a1: Int
        get() {
            return 1
        }
    var a2: Int
        get() {
            return 1
        }
        set(value) {}
    val b1: Int
        get() {
            return 1
        }
    var b2: Int
        get() {
            return 1
        }
        set(value) {}
}

// TESTCASE NUMBER: 3
open class BaseCase3(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case3() : BaseCase3(1, 1) {
    val a1: Int
        get() = 1
    var a2: Int
        get() = 1
        set(value) {}
    val b1: Int
        get() = 1
    var b2: Int
        get() = 1
        set(value) {}
}
