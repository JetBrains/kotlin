// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 26
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30507
 */
fun case_1() {
    var x: MutableList<Int>? = mutableListOf(1)
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0] += <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>[0]
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.Int>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>[0].inv()
}

// TESTCASE NUMBER: 2
fun case_2() {
    var x: MutableList<Int>? = mutableListOf(1)
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0] = <!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int>?")!>x<!>[0]<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int>?")!>x<!>
    <!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int>?")!>x<!>[0]<!>.inv()
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: MutableList<Int>? = mutableListOf(1)
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>[0] = if (true) {x=null;0} else 0
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int>?")!>x<!>
    <!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int>?")!>x<!>[0]<!>.inv()
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30507
 */
fun case_4() {
    var x: Class? = Class()
    x!!
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0, <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[0]]
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[0].inv()
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30507
 */
fun case_5() {
    var x: Class? = Class()
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>(if (true) {x=null;0} else 0, <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.fun_1()
}

// TESTCASE NUMBER: 6
fun case_6() {
    var x: MutableList<MutableList<Int>>? = mutableListOf(mutableListOf(1))
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0][<!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?")!>x<!>[0]<!>[0]] += 10
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?"), DEBUG_INFO_SMARTCAST!>x<!>[0][0].inv()
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30507
 */
fun case_7() {
    var x: Class? = Class()
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>(if (true) {x=null;0} else 0)(<!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.fun_1()
}

// TESTCASE NUMBER: 8
fun case_8() {
    var x: MutableList<MutableList<Int>>? = mutableListOf(mutableListOf(1))
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0].addAll(1, <!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?")!>x<!>[0]<!>)
}

// TESTCASE NUMBER: 9
fun case_9() {
    var x: MutableList<MutableList<Int>>? = mutableListOf(mutableListOf(1))
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0].subList(0, 2)[<!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?")!>x<!>[0]<!>[0]]
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30507
 */
fun case_10() {
    var x: MutableList<MutableList<Int>>? = mutableListOf(mutableListOf(1))
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?"), DEBUG_INFO_SMARTCAST!>x<!>.subList(if (true) {x=null;0} else 0, 2)[<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?"), DEBUG_INFO_SMARTCAST!>x<!>[0][0]]
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30507
 */
fun case_11() {
    var x: MutableList<MutableList<Int>>? = mutableListOf(mutableListOf(1))
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>> & kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0].subList(<!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.collections.MutableList<kotlin.Int>>?")!>x<!>[0]<!>[0], 2)
}
