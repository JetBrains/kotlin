// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 30
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 * ISSUES: KT-30907
 */

// TESTCASE NUMBER: 1
class Case1(val x: Any?) {
    val y = x!!
    val z: Any = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
}

// TESTCASE NUMBER: 2
class Case2(val y: Any?): ClassWithCostructorParam(y!!) {
    val z: Any = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), TYPE_MISMATCH, TYPE_MISMATCH!>y<!>
}

// TESTCASE NUMBER: 3
class Case3(val y: Any?): ClassWithCostructorParam(y as Class) {
    val z: Class = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), TYPE_MISMATCH, TYPE_MISMATCH!>y<!>
}

// TESTCASE NUMBER: 4
class Case4(val y: Any?): ClassWithCostructorParam(y!!) {
    init {
        val z: Any = <!TYPE_MISMATCH, TYPE_MISMATCH!>y<!>
    }
}

// TESTCASE NUMBER: 5
class Case5(val y: Any?): ClassWithCostructorParam(y as Interface1), Interface1 by <!TYPE_MISMATCH, TYPE_MISMATCH!>y<!> {}

// TESTCASE NUMBER: 6
fun case_6(a: Int?) = object : ClassWithCostructorParam(a!!) {
    fun run() = a<!UNSAFE_CALL!>.<!>toShort()
    init {
        println(a<!UNSAFE_CALL!>.<!>toShort())
    }
}
