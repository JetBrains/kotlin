// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun <A, B : Inv<A>, C: Out<A?>> case_1(a: C, b: B) = select(a.x, b.x)

fun case_1() {
    val x = case_1(Out(10), Inv(0.1))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 2
fun <A, B : Inv<A>, C: Out<A?>> case_2(a: C, b: B) = select(a.x, b.x)

fun case_2(y: Int) {
    val x = case_2(Out(y), Inv(0.1))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>x<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number? & kotlin.Comparable<*>?")!>this<!>
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>this<!>.funNullableAny()
        }
    }.let {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number? & kotlin.Comparable<*>?")!>it<!>
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<*> & kotlin.Number? & kotlin.Comparable<*>?")!>it<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface2? & Interface1?")!>this<!>
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableAny()
        }
    }
    x.let {
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface2? & Interface1?")!>it<!>
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableAny()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableAny()
        }
    }
    x.let {
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.itest2()
        }
    }
    x.let {
        it as Interface3
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.itest2()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & Interface2? & Interface1? & Interface2? & Interface1?")!>this<!>
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>this<!>.itest2()
        }
    }
    x.let {
        it as Interface3?
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & Interface2? & Interface1? & Interface2? & Interface1?")!>it<!>
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & Interface2 & Interface1 & Interface2? & Interface1?")!>it<!>.itest2()
        }
    }
}
