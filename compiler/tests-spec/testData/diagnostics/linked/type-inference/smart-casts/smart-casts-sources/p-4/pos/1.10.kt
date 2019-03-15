// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 4 -> sentence 1
 * RELEVANT PLACES:
 *      paragraph 1 -> sentence 2
 *      paragraph 6 -> sentence 1
 *      paragraph 9 -> sentence 3
 *      paragraph 9 -> sentence 4
 * NUMBER: 10
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality) using if expression and wrapped-unwrapped types.
 * HELPERS: classes, functions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val x = expandInv(Inv(select(10, null)))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 2
fun case_2() {
    val x = expandOut(Out(select(10, null)))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    val x = expandInv(Inv(select(10, null)))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 4
fun case_4() {
    val x = expandOut(Out(select(10, null)))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x = expandIn(In<Number?>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    val x = expandIn(In<Number?>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 7
fun <T> case_7(x: MutableMap<T?, out T?>) = select(x.values.first(), x.keys.first())

fun case_7() {
    val x = case_7(mutableMapOf(10 to 10))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 8
fun <T> case_8(x: MutableMap<T, out T>) = select(x.values.first(), x.keys.first())

fun case_8() {
    val x = case_8(mutableMapOf(10 to null))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 9
fun <T>case_9(x: MutableMap<T, out T>) = select(x.values.first(), x.keys.first())

fun case_9() {
    val x = case_9(mutableMapOf(null to 10))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 10
fun <T> case_10(x: MutableMap<T?, out T>) = select(x.values.first(), x.keys.first())

fun case_10() {
    val x = case_10(mutableMapOf(10 to 10))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 11
fun <T> case_11(x: MutableMap<T, out T?>) = select(x.values.first(), x.keys.first())

fun case_11() {
    val x = case_11(mutableMapOf(10 to 10))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 12
fun <T, K: T, M: K> case_12(x: MutableMap<M, K?>) = select(x.values.first(), x.keys.first())

fun case_12() {
    val x = case_12(mutableMapOf(10 to 11))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

/*
 * TESTCASE NUMBER: 13
 * ISSUES: KT-28334
 * NOTE: before fix of the issue type is inferred to {Int? & Byte? & Short? & Long?} (smart cast from {Int? & Byte? & Short? & Long?}?)
 */
fun <T> case_13(x: Out<T?>?, y: Out<T>) = select(x, y)

fun case_13() {
    val x = case_13(Out<Int?>(), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        val y = <!DEBUG_INFO_SMARTCAST!>x<!>.get()
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        }
    }
}

// TESTCASE NUMBER: 14
fun <T> case_14(x: Out<T>?, y: Out<T?>) = select(x, y)

fun case_14() {
    val x = case_14(Out<Int>(), Out<Int?>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        val y = <!DEBUG_INFO_SMARTCAST!>x<!>.get()
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        }
    }
}

// TESTCASE NUMBER: 15
fun <T> case_15(x: Out<T>, y: Out<T?>?) = select(x, y)

fun case_15() {
    val x = case_15(Out(), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        val y = <!DEBUG_INFO_SMARTCAST!>x<!>.get()
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        }
    }
}

/*
 * TESTCASE NUMBER: 16
 * ISSUES: KT-28334
 * NOTE: before fix of the issue type is inferred to {Int? & Byte? & Short? & Long?} (smart cast from {Int? & Byte? & Short? & Long?}?)
 */
fun <T> case_16(x: Out<T?>, y: Out<T>) = select(x, select(y, null))

fun case_16() {
    val x = case_16(Out<Int?>(), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        val y = <!DEBUG_INFO_SMARTCAST!>x<!>.get()
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        }
    }
}

// TESTCASE NUMBER: 17
fun <T> case_17(x: Out<T>, y: Out<T?>) = select(x, select(y, null))

fun case_17() {
    val x = case_17(Out<Int>(), Out<Number?>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Number?> & Out<kotlin.Number?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Number?> & Out<kotlin.Number?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        val y = <!DEBUG_INFO_SMARTCAST!>x<!>.get()
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        }
    }
}

/*
 * TESTCASE NUMBER: 18
 * ISSUES: KT-28334
 * NOTE: before fix of the issue type is inferred to {Int? & Byte? & Short? & Long?} (smart cast from {Int? & Byte? & Short? & Long?}?)
 */
fun <T> case_18(x: Out<T?>, y: Out<T>) = select(x, y)

fun case_18() {
    val x = case_18(Out<Number?>(), Out<Int>())

    <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Number?>")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Number?>")!>x<!>.equals(x)
    val y = x.get()
    if (y != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
    }
}

// TESTCASE NUMBER: 19
fun <T> case_19(x: Out<T?>, y: Out<T>) = select(x, y)

fun case_19() {
    val x = case_19(Out<Int>(), Out())

    <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?>")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?>")!>x<!>.equals(x)
    val y = x.get()
    if (y != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
    }
}

// TESTCASE NUMBER: 20
fun <T> case_20(x: Out<T>, y: Out<T>) = select(x.get(), y.get())

fun case_20(y: Int?) {
    val x = case_20(Out(y), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 21
fun <T> case_21(x: Out<T?>, y: Out<T>) = select(x.get(), y.get())

fun case_21(y: Int?) {
    val x = case_21(Out(y), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 22
fun <T> case_22(x: Out<T?>, y: Out<T>): T? = select(x.get(), y.get())

fun case_22(y: Int?) {
    val x = case_22(Out(y), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 23
fun <T> case_23(x: Out<T>, y: Out<T>): T = select(x.get(), y.get())

fun case_23(y: Int?) {
    val x = case_13(Out(y), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 24
fun <A, B : A, C: B, D: C, E: D, F> case_24(x: Out<A>, y: Out<F>) where F : E? = select(x.get(), y.get())

fun case_24(y: Int) {
    val x = case_13(Out(y), Out<Int>())

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

/*
 * TESTCASE NUMBER: 25
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29054
 */
fun <A, B : Out<A>, C: Out<B>, D: Out<C>, E: Out<D>, F> case_25(x: F, y: Out<C>) where F : Out<E?> =
    select(x.get()?.get()?.get()?.get()?.get(), y.get().get().get())

fun case_25(y: Int) {
    val x = case_25(Out(Out(Out(Out(Out(y))))), Out(Out(Out(y))))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

/*
 * TESTCASE NUMBER: 26
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29054
 */
fun <A, B : Out<A>, C: Out<B>, D: Out<C>, E: Out<D>, F> case_26(x: F, y: Out<C>) where F : Out<E?> =
    select(x.get()?.get()?.get()?.get()?.get(), y.get().get().get())

fun case_26(y: Int) {
    val x = case_26(Out(Out(Out(Out(Out(y))))), Out(Out(Out(y))))

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}
