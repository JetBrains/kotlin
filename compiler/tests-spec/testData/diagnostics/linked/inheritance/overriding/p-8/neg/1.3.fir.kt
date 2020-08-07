// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION


// TESTCASE NUMBER: 1
open class BaseCase1(val a: Int, val b: CharSequence)

open class ChildCase1 : BaseCase1(1, "") {
    override fun hashCode(): Any = TODO() //(1)
}


// TESTCASE NUMBER: 2
open class BaseCase2(val a: Int, val b: CharSequence) {
    open override fun hashCode(): Int = TODO() //(0)
}

open class ChildCase2 : BaseCase2(1, "") {
    open override fun hashCode(): Any = TODO() //(1)
}

// TESTCASE NUMBER: 3
open class BaseCase3(val a: Int, val b: CharSequence) {
    override fun hashCode(): Int = TODO() //(0)
}

open class ChildCase3 : BaseCase3(1, "") {
    override fun hashCode(): Any = TODO() //(1)
}
