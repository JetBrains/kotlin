// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 7 -> sentence 3
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: copy, componentN cannot be inherited ( in open class)
 */

// TESTCASE NUMBER: 1
open class Base1() {
    open fun copy(x: Int = 1, y: String = ""): Any = TODO()
}

<!DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR!>data<!> class Data1(val x: Int = 1, val y: String = ""): Base1()

fun case1(d: Data1){
    d.<!DEBUG_INFO_CALL("fqName: Data1.copy; typeCall: function")!>copy(1, "")<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Data1")!>d.copy(1,"")<!>
}
// TESTCASE NUMBER: 2
open class Base2() {
    open fun component1(): Any = TODO()
    open fun component2(): Boolean = TODO()
}

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Data2(val x: Int = 1, val y: String = "") : Base2()

fun case2(d: Data2){
    d.<!DEBUG_INFO_CALL("fqName: Data2.component1; typeCall: operator function"), FUNCTION_CALL_EXPECTED!>component1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>d.<!FUNCTION_CALL_EXPECTED!>component1<!><!>

    d.<!DEBUG_INFO_CALL("fqName: Data2.component2; typeCall: operator function"), FUNCTION_CALL_EXPECTED!>component2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>d.<!FUNCTION_CALL_EXPECTED!>component2<!><!>
}

// TESTCASE NUMBER: 3
open class Base3() {
    open fun component1(): Boolean = TODO()
}

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Data3(val x: Int = 1, val y: String = "") : Base3()

fun case3(d: Data3){
    d.<!DEBUG_INFO_CALL("fqName: Data3.component1; typeCall: operator function"), FUNCTION_CALL_EXPECTED!>component1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>d.<!FUNCTION_CALL_EXPECTED!>component1<!><!>

    d.<!DEBUG_INFO_CALL("fqName: Data3.component2; typeCall: operator function"), FUNCTION_CALL_EXPECTED!>component2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>d.<!FUNCTION_CALL_EXPECTED!>component2<!><!>
}

// TESTCASE NUMBER: 4
open class Base4() {
    final fun component1(): Int = TODO()
}

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Data4(val x: Int = 1, val y: String = "") : Base4()

fun case4(d: Data4){
    d.<!DEBUG_INFO_CALL("fqName: Data4.component1; typeCall: operator function"), FUNCTION_CALL_EXPECTED!>component1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>d.<!FUNCTION_CALL_EXPECTED!>component1<!><!>

    d.<!DEBUG_INFO_CALL("fqName: Data4.component2; typeCall: operator function"), FUNCTION_CALL_EXPECTED!>component2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>d.<!FUNCTION_CALL_EXPECTED!>component2<!><!>
}


// TESTCASE NUMBER: 5
open class Base5() {
    final fun copy(x: Int = 1, y: String = ""): Any = TODO()
}

<!DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR!>data<!> class Data5(val x: Int = 1, val y: String = ""): Base5()

fun case5(d: Data5){
    d.<!DEBUG_INFO_CALL("fqName: Data5.copy; typeCall: function")!>copy(1, "")<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Data5")!>d.copy(1,"")<!>
}