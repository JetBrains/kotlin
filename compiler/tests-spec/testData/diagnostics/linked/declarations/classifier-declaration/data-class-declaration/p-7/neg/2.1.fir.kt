// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION




// TESTCASE NUMBER: 1
open class Base1() {
    internal fun toString(): String = TODO()
}

data class Data1(val x: Int = 1, val y: String = ""): Base1()

fun case1(d: Data1){
    d.<!DEBUG_INFO_CALL("fqName: Base1.toString; typeCall: function")!>toString()<!>
    d.toString()
}

// TESTCASE NUMBER: 2
open class Base2() {
    protected fun toString(): String = TODO()
}

data class Data2(val x: Int = 2, val y: String = ""): Base2()

fun case2(d: Data2){
    d.<!DEBUG_INFO_CALL("fqName: kotlin.toString; typeCall: extension function")!>toString()<!>
    d.toString()
}
// TESTCASE NUMBER: 3
open class Base3() {
    private fun toString(): String = TODO()
}

data class Data3(val x: Int = 2, val y: String = ""): Base3()

fun case3(d: Data3){
    d.<!DEBUG_INFO_CALL("fqName: kotlin.toString; typeCall: extension function")!>toString()<!>
    d.toString()
}
// TESTCASE NUMBER: 4
open class Base4() {
    public fun toString(): Int = TODO()
}

data class Data4(val x: Int = 2, val y: String = ""): Base4()
