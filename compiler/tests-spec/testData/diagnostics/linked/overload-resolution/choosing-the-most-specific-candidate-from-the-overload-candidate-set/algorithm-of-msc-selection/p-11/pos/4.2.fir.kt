// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

fun case1() {
    //to (1.1)
    1.<!DEBUG_INFO_CALL("fqName: testPackCase1.boo; typeCall: extension function")!>boo(1)<!>
    //(1.1) return type is String
    1.boo(1)
    //to (1.1)
    1.<!DEBUG_INFO_CALL("fqName: testPackCase1.boo; typeCall: extension function")!>boo(x = 1)<!>
    //(1.1) return type is String
    1.boo(x = 1)

}

private fun Int.boo(x: Int, a: Any = ""): String = TODO() //(1.1)
private fun Int.boo(x: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.1)
private fun Long.boo(x: Long, a: Any = ""): Unit = TODO() //(1.2)
private fun Long.boo(x: Long, a: Any = "", b: Any = 1): Unit = TODO() //(1.2)
private fun Short.boo(x: Short, a: Any = "", b: Any = 1): Unit = TODO() //(1.3)
private fun Short.boo(x: Short, a: Any = ""): Unit = TODO() //(1.3)
private fun Byte.boo(x: Byte, a: Any = "", b: Any = 1): Unit = TODO() //(1.4)
private fun Byte.boo(x: Byte, a: Any = ""): Unit = TODO() //(1.4)

// FILE: TestCase1.kt
// TESTCASE NUMBER: 2
package testPackCase2
import testPackCase2.Case2.Companion.boo

class Case2 {
    val boo: Int = 1

    companion object {
        fun Int.boo(x: Int, y: Int, a: Any = ""): String = TODO() //(1.1)
        fun Int.boo(x: Int, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.1)
        fun Long.boo(x: Long, y: Int, a: Any = ""): Unit = TODO() //(1.2)
        fun Long.boo(x: Long, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.2)
        fun Short.boo(x: Short, y: Int, a: Any = ""): Unit = TODO() //(1.3)
        fun Short.boo(x: Short, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.3)
        fun Byte.boo(x: Byte, y: Int, a: Any = ""): Unit = TODO() //(1.4)
        fun Byte.boo(x: Byte, y: Int, a: Any = "", b: Any = 1): Unit = TODO() //(1.4)
    }

    fun case() {
        1.<!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.Companion.boo; typeCall: extension function")!>boo(1, 1)<!>
        1.boo(1, 1)
    }
}

fun case2(case: Case2) {
    //to (1.1)
    case.boo.<!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.Companion.boo; typeCall: extension function")!>boo(1, 2)<!>
    //(1.1) return type is String
    case.boo.boo(1, 2)
    //to (1.1)
    case.boo.<!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.Companion.boo; typeCall: extension function")!>boo(x = 1, y = 2)<!>
    //(1.1) return type is String
    case.boo.boo(x = 1, y = 2)

    case.apply { 1.<!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.Companion.boo; typeCall: extension function")!>boo(1, 1)<!> }
    case.apply { 1.boo(1, 1) }

    case.let { 1.<!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.Companion.boo; typeCall: extension function")!>boo(1, 1)<!> }
    case.let { 1.boo(1, 1) }

    case.also { 1.<!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.Companion.boo; typeCall: extension function")!>boo(1, 1)<!> }
    case.also { 1.boo(1, 1) }

    case.run { 1.<!DEBUG_INFO_CALL("fqName: testPackCase2.Case2.Companion.boo; typeCall: extension function")!>boo(1, 1)<!> }
    case.run { 1.boo(1, 1) }
}

// FILE: TestCase1.kt
// TESTCASE NUMBER: 3
package testPackCase3
import testPackCase3.Case3.Companion.get

class Case3 {
    val boo: Int = 1

    companion object {
        operator fun Int.get(x: String, a: Any = "", b: Any = 1): Unit  = TODO() //(1.1)
        operator fun Int.get(x: String, a: Any = ""): String = TODO() //(1.1)
        operator fun Long.get(x: String, a: Any = "", b: Any = 1): Unit = TODO() //(1.2)
        operator fun Long.get(x: String, a: Any = ""): Unit = TODO() //(1.2)
        operator fun Short.get(x: String, a: Any = "", b: Any = 1): Unit = TODO() //(1.3)
        operator fun Short.get(x: String, a: Any = ""): Unit = TODO() //(1.3)
        operator fun Byte.get(x: String, a: Any = "", b: Any = 1): Unit = TODO() //(1.4)
        operator fun Byte.get(x: String, a: Any = "" ): Unit = TODO() //(1.4)
    }

    fun case() {
        1["1"]
        1["1"]
    }
}

fun case3(case: Case3) {
    //to (1.1)
    case.boo["1"]
    //(1.1) return type is String
    case.boo["1"]

    case.apply { 1["1"] }
    case.apply { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>1["1"]<!> }

    case.let { 1["1"] }
    case.let { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>1["1"]<!> }

    case.also { 1["1"] }
    case.also { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>1["1"]<!> }

    case.run { 1["1"] }
    case.run { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>1["1"]<!> }
}
