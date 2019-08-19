// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 11
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, functions, interfaces, properties
 */

// TESTCASE NUMBER: 1
fun <A, B : Inv<A>, C: Out<A?>> case_1(a: C, b: B) = select(a.x, b.x)

fun case_1() {
    val x = case_1(Out(10), Inv(0.1))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 2
fun <A, B : Inv<A>, C: Out<A?>> case_2(a: C, b: B) = select(a.x, b.x)

fun case_2(y: Int) {
    val x = case_2(Out(y), Inv(0.1))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Int}> & Number} & {Comparable<{Double & Int}> & Number}?")!>x<!>.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-28670
 */
fun case_3(a: Int?, b: Float?, c: Double?, d: Boolean?) {
    when (d) {
        true -> a
        false -> b
        null -> c
    }.apply {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}")!>this<!>
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}")!>this<!>.funNullableAny()
        }
    }.let {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}>? & Number?}")!>it<!>
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}"), DEBUG_INFO_SMARTCAST!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Double & Float & Int}> & Number} & {Comparable<{Double & Float & Int}>? & Number?}")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-28670
 */
fun case_4(a: Interface1?, b: Interface2?, c: Boolean) {
    a as Interface2?
    b as Interface1?
    val x = when (c) {
        true -> a
        false -> b
    }

    x.apply {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableAny()
        }
    }
    x.let {
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>it<!>
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-28670
 */
fun case_5(a: Interface1?, b: Interface2?, d: Boolean) {
    a as Interface2?
    b as Interface1
    val x = when (d) {
        true -> a
        false -> b
    }

    x.apply {
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableAny()
        }
    }
    x.let {
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-28670
 */
fun case_6(a: Interface1?, b: Interface2, d: Boolean) {
    a as Interface2?
    b as Interface1
    val x = when (d) {
        true -> a
        false -> b
    }

    x.apply {
        this as Interface3
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>
        if (<!SENSELESS_COMPARISON!>this != null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.itest2()
        }
    }
    x.let {
        it as Interface3
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>
        if (<!SENSELESS_COMPARISON!>it != null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.itest2()
        }
    }
}

/*
 * TESTCASE NUMBER: 7
 * ISSUES: KT-28670
 */
fun case_7(a: Interface1?, b: Interface2?, d: Boolean) {
    a as Interface2?
    b as Interface1?
    val x = when (d) {
        true -> a
        false -> b
    }

    x.apply {
        this as Interface3?
        <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>this<!>.itest2()
        }
    }
    x.let {
        it as Interface3?
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & {Interface1? & Interface2?}")!>it<!>
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & {Interface1 & Interface2} & {Interface1? & Interface2?}")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2} & {Interface1? & Interface2?}"), DEBUG_INFO_SMARTCAST!>it<!>.itest2()
        }
    }
}
