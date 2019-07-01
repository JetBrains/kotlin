// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 58
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18532
 */
fun case_1() {
    val x = In<Int>()
    val y: In<*>
    y = x
    y.put(0)
    val z: In<*> = x
    z.put(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0<!>)
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18532
 */
fun case_2() {
    val x = Inv<Int>()
    val y: Inv<out Number>
    y = x
    y.put(0)
    val z: Inv<out Number> = x
    z.put(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0<!>)
}

// TESTCASE NUMBER: 3
fun case_3() {
    val x = Inv<Number>()
    val y: Inv<Number>
    y = x
    y.put(0)
    val z: Inv<Number> = x
    z.put(0)
}

// TESTCASE NUMBER: 4
fun case_4() {
    val x = In<Number>()
    val y: In<Number>
    y = x
    y.put(0)
    val z: In<Number> = x
    z.put(0)
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18532
 */
fun case_5() {
    val x = Inv<Int>()
    var y: Inv<out Number> = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>Inv<Int>()<!>
    y = x
    y.put(0)
    val z: Inv<out Number> = x
    z.put(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0<!>)
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18532
 */
fun case_6() {
    val x = Inv<Int>()
    var y: Inv<out Number> = Inv<Int>()
    if (true)
        y = x
    y.put(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0<!>)
    val z: Inv<out Number> = x
    z.put(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0<!>)
}