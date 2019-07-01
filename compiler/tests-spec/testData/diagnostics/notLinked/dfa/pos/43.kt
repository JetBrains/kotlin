// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 43
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-28670
 */
fun case_1(a: Interface1?, b: Interface2?) {
    b as Interface1?
    a as Interface2?
    val c = select(a, b)
    if (c != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest2()
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-28670
 */
fun case_2(a: Interface1?, b: Interface2?) {
    b as Interface1?
    a as Interface2?

    select(a, b)!!.run {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>this<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>this<!>.itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>this<!>.itest2()
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-28670
 */
fun case_3(a: Interface1?, b: Interface2?) {
    b as Interface1?
    a as Interface2?

    val c = select(a, b)!!
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest()
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest1()
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest2()
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-28670
 */
fun case_4(a: Interface1?, b: Interface2?) {
    b as Interface1?
    a as Interface2?

    val c = select(a, b) ?: return
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest()
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest1()
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest2()
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-28670
 */
fun case_5(a: Interface1?, b: Interface2?) {
    b as Interface1?
    a as Interface2?

    val foo = l1@ fun(): Any {
        val bar = l2@ fun() {
            val c = select(a, b) ?: return@l2
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}")!>c<!>.itest2()
        }
        return bar
    }
    println(foo)
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-28670
 */
fun case_6(a: Interface1?, b: Interface2?) {
    b as Interface1?
    a as Interface2?

    val c = select(a, b)
    c ?: return
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest()
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest1()
    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest2()
}

/*
 * TESTCASE NUMBER: 7
 * ISSUES: KT-28670
 */
fun case_7(a: Interface1?, b: Interface2?) {
    b as Interface1?
    a as Interface2?

    val foo = l1@ fun(): Any {
        val bar = l2@ fun() {
            val c = select(a, b)
            c ?: return@l2
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>c<!>.itest2()
        }
        return bar
    }
    println(foo)
}
