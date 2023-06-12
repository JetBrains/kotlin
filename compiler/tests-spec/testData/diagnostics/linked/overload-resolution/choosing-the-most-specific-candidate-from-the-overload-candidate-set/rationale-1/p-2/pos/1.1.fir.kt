// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, rationale-1 -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The most specific callable can forward itself to any other callable from the overload candidate set, while the opposite is not true.
 */

// TESTCASE NUMBER: 1
class Case1 {
    fun <T> List<Any>.foo(x: T): Unit = TODO()
    fun <T> List<Number>.foo(x: T): String = TODO()

    fun <T : Number> case(list: List<T>, x: T) {
        list.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: extension function")!>foo(x)<!>
        list.foo(x)
    }

}

// TESTCASE NUMBER: 2
class Case2 {
    fun <T> List<T>.foo(x: Int?): Unit = TODO()
    fun <T> List<T>.foo(x: Int): String = TODO()

    fun case(list: List<Any>) {
        list.<!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: extension function")!>foo(1)<!>
        list.foo(1)
    }
}

// TESTCASE NUMBER: 3
class Case3 {
    fun List<Any>.foo(x: Any): String = TODO()

    fun <T> List<Any>.foo(x: T): Unit = TODO()

    fun case(list: List<Int>) {
        list.<!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: extension function")!>foo('a')<!>
        list.foo('a')
    }
}

// TESTCASE NUMBER: 4
class Case4 {
    fun <T> List<T>.foo(x: T, y: Any): Unit = TODO()

    fun <T> List<T>.foo(x: T, y: CharSequence): Unit = TODO()

    fun <T> List<T>.foo(x: T, y: String): String = TODO()

    fun case(list: List<Short>, x: Any) {
        list.<!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: extension function")!>foo(x, "str")<!>
        list.foo(x, "str")
    }
}

// TESTCASE NUMBER: 5
class Case5 {
    class Child : Parent1, Parent2

    interface Parent1
    interface Parent2 : Parent1

    fun foo(x: Parent1) {} //(1)
    fun foo(y: Parent2, z: String = "foo"): String = TODO() //2

    fun testcase1() {
        <!DEBUG_INFO_CALL("fqName: Case5.foo; typeCall: function")!>foo(Child())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(Child())<!>
    }
}
// TESTCASE NUMBER: 6
class Case6 {
    class Child : Parent1, Parent2

    interface Parent1
    interface Parent2 : Parent1

    fun foo(x: Parent1, z: String = "foo") {} //(1)
    fun foo(y: Parent2): String = TODO() //2

    fun testcase1() {
        //foo(Child())
        <!DEBUG_INFO_CALL("fqName: Case6.foo; typeCall: function")!>foo(Child())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(Child())<!>
    }
}
