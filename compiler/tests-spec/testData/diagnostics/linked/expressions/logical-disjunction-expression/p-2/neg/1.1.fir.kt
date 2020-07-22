// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// MODULE: libModule
// FILE: libModule/JavaClass.java
package libModule;

public class JavaClass {
    public static Object VALUE = false;
}

// MODULE: mainModule(libModule)
// FILE: KotlinClass.kt
package mainModule
import libModule.*
import checkSubtype
import checkType
import check

// TESTCASE NUMBER: 0
fun foo() = run { false || JavaClass.VALUE || throw Exception() }

// TESTCASE NUMBER: 1
fun case1() {
    val a: Boolean? = false
    checkSubtype<Boolean?>(a)
    val x4 = a || true
    x4 <!AMBIGUITY!>checkType<!> { <!NONE_APPLICABLE!>check<!><Boolean>() }
}

// TESTCASE NUMBER: 2
fun case2() {
    val a: Any = false
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>a<!>
    val x4 = a || true
    x4 <!AMBIGUITY!>checkType<!> { <!NONE_APPLICABLE!>check<!><Boolean>() }
}

// TESTCASE NUMBER: 3
fun case3() {
    val a1 = false
    val a2 = JavaClass.VALUE
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any..kotlin.Any?!")!>a2<!>

    val x3 = a1 || a2
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>x3<!>

    x3 <!AMBIGUITY!>checkType<!> { <!NONE_APPLICABLE!>check<!><Boolean>() }
}

// TESTCASE NUMBER: 4
fun case4() {
    var x = false ||<!SYNTAX!><!> ;
}

// TESTCASE NUMBER: 5
fun case5() {
    var y = false ||<!SYNTAX!><!>
}

// TESTCASE NUMBER: 5
fun case5() {
    var x =<!SYNTAX!><!> <!SYNTAX!>||<!>
}

// TESTCASE NUMBER: 6
fun case6() {
    var x =<!SYNTAX!><!> <!SYNTAX!>|| false  || true<!>
}
