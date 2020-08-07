// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
open class BaseCase1(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case1a() : BaseCase1(1, 1) {
    override val a1: Any = 1
    override var a2: Any = 1
    override val b1: Any = 1
    override var b2: Any = 1
}

open class Case1b() : BaseCase1(1, 1) {
    override val a1 = "1"
    override var a2 = "1"
    override val b1 = "1"
    override var b2 = "1"
}

// TESTCASE NUMBER: 2
open class BaseCase2(open val a1: Int, open var a2: Int) {
    open val b1: Int = 1
    open var b2: Int = 1
}

open class Case2() : BaseCase2(1, 1) {
    override val a1: Any
        get() {
            return 1
        }
    override var a2: Any
        get() {
            return 1
        }
        set(value) {}
    override val b1: Any
        get() {
            return 1
        }
    override var b2: Any
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
    override val a1: Any
        get() = 1
    override var a2: Any
        get() = 1
        set(value) {}
    override val b1: Any
        get() = 1
    override var b2: Any
        get() = 1
        set(value) {}
}
