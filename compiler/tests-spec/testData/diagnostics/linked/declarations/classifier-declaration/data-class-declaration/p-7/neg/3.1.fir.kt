// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION


// TESTCASE NUMBER: 1
open class Base1() {
    open fun copy(x: Int = 1, y: String = ""): Any = TODO()
}

data class Data1(val x: Int = 1, val y: String = ""): Base1()

fun case1(d: Data1){
    d.<!DEBUG_INFO_CALL("fqName: Data1.copy; typeCall: function")!>copy(1, "")<!>
    d.copy(1,"")
}
// TESTCASE NUMBER: 2
open class Base2() {
    open fun component1(): Any = TODO()
    open fun component2(): Boolean = TODO()
}

data class Data2(val x: Int = 1, val y: String = "") : Base2()

fun case2(d: Data2){
    d.<!UNRESOLVED_REFERENCE!>component1<!>
    d.<!UNRESOLVED_REFERENCE!>component1<!>

    d.<!UNRESOLVED_REFERENCE!>component2<!>
    d.<!UNRESOLVED_REFERENCE!>component2<!>
}

// TESTCASE NUMBER: 3
open class Base3() {
    open fun component1(): Boolean = TODO()
}

data class Data3(val x: Int = 1, val y: String = "") : Base3()

fun case3(d: Data3){
    d.<!UNRESOLVED_REFERENCE!>component1<!>
    d.<!UNRESOLVED_REFERENCE!>component1<!>

    d.<!UNRESOLVED_REFERENCE!>component2<!>
    d.<!UNRESOLVED_REFERENCE!>component2<!>
}

// TESTCASE NUMBER: 4
open class Base4() {
    final fun component1(): Int = TODO()
}

data class Data4(val x: Int = 1, val y: String = "") : Base4()

fun case4(d: Data4){
    d.<!UNRESOLVED_REFERENCE!>component1<!>
    d.<!UNRESOLVED_REFERENCE!>component1<!>

    d.<!UNRESOLVED_REFERENCE!>component2<!>
    d.<!UNRESOLVED_REFERENCE!>component2<!>
}


// TESTCASE NUMBER: 5
open class Base5() {
    final fun copy(x: Int = 1, y: String = ""): Any = TODO()
}

data class Data5(val x: Int = 1, val y: String = ""): Base5()

fun case5(d: Data5){
    d.<!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!><!AMBIGUITY!>copy<!>(1, "")<!>
    d.<!AMBIGUITY!>copy<!>(1,"")
}
