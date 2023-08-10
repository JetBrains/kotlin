// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, logical-disjunction-expression -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: expressions, logical-disjunction-expression -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Both operands of a logical disjunction expression must have a type which is a subtype of kotlin.Boolean, otherwise it is a type error.
 * HELPERS: checkType
 */

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
fun foo() = run { false || <!CONDITION_TYPE_MISMATCH!>JavaClass.VALUE<!> || throw Exception() }

// TESTCASE NUMBER: 1
fun case1() {
    val a: Boolean? = false
    checkSubtype<Boolean?>(a)
    val x4 = <!CONDITION_TYPE_MISMATCH!>a<!> || true
    x4 checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 2
fun case2() {
    val a: Any = false
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>a<!>
    val x4 = <!CONDITION_TYPE_MISMATCH!>a<!> || true
    x4 checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 3
fun case3() {
    val a1 = false
    val a2 = JavaClass.VALUE
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any..kotlin.Any?!")!>a2<!>

    val x3 = a1 || <!CONDITION_TYPE_MISMATCH!>a2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>x3<!>

    x3 checkType { check<Boolean>() }
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
