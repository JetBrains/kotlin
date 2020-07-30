// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION




// TESTCASE NUMBER: 1
open class Base1() {
   override final public fun toString(): String = TODO()
}

data class Data1(val x: Int = 1, val y: String = ""): Base1()

fun case1(d: Data1){
    d.<!DEBUG_INFO_CALL("fqName: Base1.toString; typeCall: function")!>toString()<!>
    d.toString()
}

// TESTCASE NUMBER: 2
open class Base2() {
    override final fun toString(): String = TODO()
}

data class Data2(val x: Int = 2, val y: String = ""): Base2()

fun case2(d: Data2){
    d.<!DEBUG_INFO_CALL("fqName: Base2.toString; typeCall: function")!>toString()<!>
    d.toString()
}
