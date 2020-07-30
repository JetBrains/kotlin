// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 7 -> sentence 2
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION:  correct explicit implementation is not available in open class
 */



// TESTCASE NUMBER: 1
open class Base1() {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>internal<!> fun <!VIRTUAL_MEMBER_HIDDEN!>toString<!>(): String = TODO()
}

data class Data1(val x: Int = 1, val y: String = ""): Base1()

fun case1(d: Data1){
    d.<!DEBUG_INFO_CALL("fqName: Data1.toString; typeCall: function")!>toString()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>d.toString()<!>
}

// TESTCASE NUMBER: 2
open class Base2() {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>protected<!> fun <!VIRTUAL_MEMBER_HIDDEN!>toString<!>(): String = TODO()
}

data class Data2(val x: Int = 2, val y: String = ""): Base2()

fun case2(d: Data2){
    d.<!DEBUG_INFO_CALL("fqName: kotlin.toString; typeCall: extension function")!>toString()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>d.toString()<!>
}
// TESTCASE NUMBER: 3
open class Base3() {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>private<!> fun <!VIRTUAL_MEMBER_HIDDEN!>toString<!>(): String = TODO()
}

data class Data3(val x: Int = 2, val y: String = ""): Base3()

fun case3(d: Data3){
    d.<!DEBUG_INFO_CALL("fqName: kotlin.toString; typeCall: extension function")!>toString()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>d.toString()<!>
}
// TESTCASE NUMBER: 4
open class Base4() {
    public fun <!VIRTUAL_MEMBER_HIDDEN!>toString<!>(): Int = TODO()
}

data class Data4(val x: Int = 2, val y: String = ""): Base4()
