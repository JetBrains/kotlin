// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK:  overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 11 -> sentence 4
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 3
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 11 -> sentence 4
 * built-in-types-and-their-semantics, built-in-integer-types-1, integer-type-widening -> paragraph 3 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: call with implicit receiver: different built-in integer types and one of them is kotlin.Int
 */

// TESTCASE NUMBER: 1
class Case1

fun case1(case: Case1) {
    case.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: boo; typeCall: extension function")!>boo(1)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(1)<!>
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: boo; typeCall: extension function")!>boo(x = 1)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(x = 1)<!>
    }
}

fun Case1.boo(x: Int, a: Any = ""): String = TODO() //(1.1)
fun Case1.boo(x: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.1)
fun Case1.boo(x: Long, a: Any = ""): Unit = TODO() //(1.2)
fun Case1.boo(x: Long, a: Any = "", b: Any = 1): Unit = TODO() //(1.2)
fun Case1.boo(x: Short, a: Any = ""): Unit = TODO() //(1.3)
fun Case1.boo(x: Short, a: Any = "", b: Any = 1): Unit = TODO() //(1.3)
fun Case1.boo(x: Byte, a: Any = ""): Unit = TODO() //(1.4)
fun Case1.boo(x: Byte, a: Any = "", b: Any = 1): Unit = TODO() //(1.4)

// TESTCASE NUMBER: 2
class Case2 {
    fun boo(x: Int, y: Int, a: Any = ""): String = TODO() //(1.1)
    fun boo(x: Int, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.1)
    fun boo(x: Long, y: Int, a: Any = ""): Unit = TODO() //(1.2)
    fun boo(x: Long, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.2)
    fun boo(x: Short, y: Int, a: Any = ""): Unit = TODO() //(1.3)
    fun boo(x: Short, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.3)
    fun boo(x: Byte, y: Int, a: Any = ""): Unit = TODO() //(1.4)
    fun boo(x: Byte, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.4)
}

fun case2(case: Case2) {
    case.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case2.boo; typeCall: function")!>boo(1, 2)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(1, 2)<!>
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case2.boo; typeCall: function")!>boo(x = 1, y = 2)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(x = 1, y = 2)<!>
    }
}

// TESTCASE NUMBER: 3
class Case3 {
    fun boo(x: Int, y: Int, a: Any = ""): String = TODO() //(1.1)
    fun boo(x: Int, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.1)
    fun boo(x: Int, y: Short, a: Any = ""): Unit = TODO() //(1.2)
    fun boo(x: Int, y: Short, a: Any = "", b: Any = 1): Unit = TODO() //(1.2)
}

fun case3(case: Case3) {
    case.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case3.boo; typeCall: function")!>boo(1, 2)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(1, 2)<!>
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case3.boo; typeCall: function")!>boo(x = 1, y = 2)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(x = 1, y = 2)<!>
    }
}


// TESTCASE NUMBER: 4
class Case4 {
    operator fun get(x: Int, a: Any = ""): String = TODO() //(1.1)
    operator fun get(x: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.1)
    operator fun get(x: Long, a: Any = "", b: Any = 1): Unit = TODO() //(1.2)
    operator fun get(x: Long, a: Any = ""): Unit = TODO() //(1.2)
    operator fun get(x: Short, a: Any = ""): Unit = TODO() //(1.3)
    operator fun get(x: Short, a: Any = "", b: Any = 1): Unit = TODO() //(1.3)
    operator fun get(x: Byte, a: Any = ""): Unit = TODO() //(1.4)
    operator fun get(x: Byte, a: Any = "", b: Any = 1): Unit = TODO() //(1.4)
}

fun case4(case: Case4) {
    case.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case4.get; typeCall: operator function")!>get(1)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>get(1)<!>
        //to (1.1)
        this [1]
        //(1.1) return type is String
        this [1]
    }
}
