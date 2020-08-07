// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
open class BaseCase1(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case1a() : BaseCase1(1, 1) {
    override val a1: Int = 1
    override var a2: Int = 1
    override val b1: Int = 1
    override var b2: Int = 1
}

open class Case1b() : BaseCase1(1, 1) {
    override val a1 = 1
    override var a2 = 1
    override val b1 = 1
    override var b2 = 1
}

fun case1a(b: BaseCase1, ca: Case1a, cb: Case1b) {
    b.a1
    b.a2
    b.b1
    b.b2

    ca.a1
    ca.a2
    ca.b1
    ca.b2

    cb.a1
    cb.a2
    cb.b1
    cb.b2
}


// TESTCASE NUMBER: 2
open class BaseCase2(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case2() : BaseCase2(1, 1) {
    override val a1: Int
        get() {
            return 1
        }
    override var a2: Int
        get() {
            return 1
        }
        set(value) {}
    override val b1: Int
        get() {
            return 1
        }
    override var b2: Int
        get() {
            return 1
        }
        set(value) {}
}

fun case2(b: BaseCase2, c: Case2) {
    b.a1
    b.a2
    b.b1
    b.b2

    c.a1
    c.a2
    c.b1
    c.b2
}

// TESTCASE NUMBER: 3
open class BaseCase3(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case3() : BaseCase3(1, 1) {
    override val a1: Int
        get() = 1
    override var a2: Int
        get() = 1
        set(value) {}
    override val b1: Int
        get() = 1
    override var b2: Int
        get() = 1
        set(value) {}
}

fun case3(b: BaseCase3, c: Case3) {
    b.a1
    b.a2
    b.b1
    b.b2

    c.a1
    c.a2
    c.b1
    c.b2
}
