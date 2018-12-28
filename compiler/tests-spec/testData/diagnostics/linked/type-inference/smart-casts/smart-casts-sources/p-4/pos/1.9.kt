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
 * NUMBER: 9
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality) using if expression and generic types with projections.
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(x: Out<<!REDUNDANT_PROJECTION!>out<!> Int?>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.equals(x)
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28598
 */
fun case_2(a: Out<<!REDUNDANT_PROJECTION!>out<!> Out<<!REDUNDANT_PROJECTION!>out<!> Out<<!REDUNDANT_PROJECTION!>out<!> Out<<!REDUNDANT_PROJECTION!>out<!> Out<<!REDUNDANT_PROJECTION!>out<!> Out<<!REDUNDANT_PROJECTION!>out<!> Int?>?>?>?>?>?>?) {
    if (a != null) {
        val b = <!DEBUG_INFO_EXPRESSION_TYPE("Out<out Out<out Out<out Out<out Out<out Out<out kotlin.Int?>?>?>?>?>?> & Out<out Out<out Out<out Out<out Out<out Out<out kotlin.Int?>?>?>?>?>?>?")!>a<!>.get()
        if (b != null) {
            val c = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<Out<kotlin.Int?>?>?>?>?> & Out<Out<Out<Out<Out<kotlin.Int?>?>?>?>?>?"), DEBUG_INFO_SMARTCAST!>b<!>.get()
            if (c != null) {
                val d = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<kotlin.Int?>?>?>?> & Out<Out<Out<Out<kotlin.Int?>?>?>?>?"), DEBUG_INFO_SMARTCAST!>c<!>.get()
                if (d != null) {
                    val e = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<kotlin.Int?>?>?> & Out<Out<Out<kotlin.Int?>?>?>?"), DEBUG_INFO_SMARTCAST!>d<!>.get()
                    if (e != null) {
                        val f = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<kotlin.Int?>?> & Out<Out<kotlin.Int?>?>?"), DEBUG_INFO_SMARTCAST!>e<!>.get()
                        if (f != null) {
                            val g = <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>f<!>.get()
                            if (g != null) {
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>g<!>
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>g<!>.equals(g)
                            }
                        }
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 3
fun case_3(a: Inv<out Int>?) {
    if (a != null) {
        val b = a
        if (<!SENSELESS_COMPARISON!>a == null<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>b<!>.equals(b)
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: Out<<!REDUNDANT_PROJECTION!>out<!> Int>?, b: Out<<!REDUNDANT_PROJECTION!>out<!> Int> = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>a<!> else Out<Int>()) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.equals(b)
}

// TESTCASE NUMBER: 5
val x: Out<<!REDUNDANT_PROJECTION!>out<!> Int>? = null

fun case_5() {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    val x: Inv<out Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Out<<!REDUNDANT_PROJECTION!>out<!> Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.equals(x)
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25432
 */
fun case_8(x: ClassWithThreeTypeParameters<out Int?, out Short?, ClassWithThreeTypeParameters<out Int?, out Short?, out String?>?>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.equals(x)
        if (x.x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x.x<!><!UNSAFE_CALL!>.<!>equals(x.x)
        }
        if (x.y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short?")!>x.y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short?")!>x.y<!><!UNSAFE_CALL!>.<!>equals(x.y)
        }
        if (x.z != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?")!>x.z<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?")!>x.z<!><!UNSAFE_CALL!>.<!>equals(x.z)
            if (x.z<!UNSAFE_CALL!>.<!>x != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x.z<!UNSAFE_CALL!>.<!>x<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x.z<!UNSAFE_CALL!>.<!>x<!><!UNSAFE_CALL!>.<!>equals(x.z<!UNSAFE_CALL!>.<!>x)
            }
            if (x.z<!UNSAFE_CALL!>.<!>y != null && x.z<!UNSAFE_CALL!>.<!>z != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short?")!>x.z<!UNSAFE_CALL!>.<!>y<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.z<!UNSAFE_CALL!>.<!>z<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short?")!>x.z<!UNSAFE_CALL!>.<!>y<!><!UNSAFE_CALL!>.<!>equals(x.z<!UNSAFE_CALL!>.<!>y)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x.z<!UNSAFE_CALL!>.<!>z<!>.equals(x.z<!UNSAFE_CALL!>.<!>z)
            }
        }
    }
}

// TESTCASE NUMBER: 9
fun case_9(a: (Inv<out Int>) -> Inv<out Int>?, b: Inv<out Int>?) {
    if (b != null) {
        val c = a(<!DEBUG_INFO_SMARTCAST!>b<!>)
        if (c != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.equals(c)
        }
    }
}

// TESTCASE NUMBER: 10
fun case_9(a: Inv<*>?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.equals(a)
    }
}

// TESTCASE NUMBER: 11
fun case_10() {
    val a10: Out<*>? = null

    if (a10 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.equals(a10)
    }
}

// TESTCASE NUMBER: 12
fun case_11() {
    val a: In<*>? = null

    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.equals(a)
    }
}

// TESTCASE NUMBER: 13
fun case_13(a: ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.equals(a)
    }
}

// TESTCASE NUMBER: 14
fun case_14(a: ClassWithSixTypeParameters<*, Int, *, Out<*>, *, Map<Float, Out<*>>>?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.equals(a)
    }
}

// TESTCASE NUMBER: 15
fun case_15(a: Any?) {
    if (a is ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
        if (a != null) {
            a
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
        }
    }
}

// TESTCASE NUMBER: 16
fun case_16(a: Any?) {
    if (a === null) {
    } else {
        if (a !is ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any & kotlin.Any?")!>a<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
        }
    }
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28598
 */
fun case_17(a: Inv<out Out<Out<Out<Out<Out<Int?>?>?>?>?>?>?) {
    if (a != null) {
        val b = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Out<Out<Out<Out<Out<kotlin.Int?>?>?>?>?>?> & Inv<out Out<Out<Out<Out<Out<kotlin.Int?>?>?>?>?>?>?")!>a<!>.get()
        if (b != null) {
            val c = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<Out<kotlin.Int?>?>?>?>?> & Out<Out<Out<Out<Out<kotlin.Int?>?>?>?>?>?"), DEBUG_INFO_SMARTCAST!>b<!>.get()
            if (c != null) {
                val d = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<kotlin.Int?>?>?>?> & Out<Out<Out<Out<kotlin.Int?>?>?>?>?"), DEBUG_INFO_SMARTCAST!>c<!>.get()
                if (d != null) {
                    val e = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<kotlin.Int?>?>?> & Out<Out<Out<kotlin.Int?>?>?>?"), DEBUG_INFO_SMARTCAST!>d<!>.get()
                    if (e != null) {
                        val f = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<kotlin.Int?>?> & Out<Out<kotlin.Int?>?>?"), DEBUG_INFO_SMARTCAST!>e<!>.get()
                        if (f != null) {
                            val g = <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int?> & Out<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>f<!>.get()
                            if (g != null) {
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>g<!>
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>g<!>.equals(g)
                            }
                        }
                    }
                }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_18(a: Inv<out Int?>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a.x<!><!UNSAFE_CALL!>.<!>equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 19
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_19(a: Inv<out Nothing?>) {
    if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>a.x<!>.equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 20
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_20(a: Inv<out Any?>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!><!UNSAFE_CALL!>.<!>equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 21
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_21(a: Out<<!REDUNDANT_PROJECTION!>out<!> Int?>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a.x<!><!UNSAFE_CALL!>.<!>equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 22
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_22(a: Out<<!REDUNDANT_PROJECTION!>out<!> Nothing?>) {
    if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>a.x<!>.equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 23
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_23(a: Out<<!REDUNDANT_PROJECTION!>out<!> Any?>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!><!UNSAFE_CALL!>.<!>equals(a.x)
    }
}

// TESTCASE NUMBER: 24
fun case_24(a: Out<Int?>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
    }
}

// TESTCASE NUMBER: 25
fun case_25(a: Out<Nothing?>) {
    if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>a.x<!>.equals(a.x)
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: Out<Any?>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 27
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_27(a: Inv<in Any>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!><!UNSAFE_CALL!>.<!>equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 28
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_28(a: Inv<in Float>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!><!UNSAFE_CALL!>.<!>equals(a.x)
    }
}

/*
 * TESTCASE NUMBER: 29
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_29(a: Inv<in Nothing>) {
    if (a.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a.x<!><!UNSAFE_CALL!>.<!>equals(a.x)
    }
}

// TESTCASE NUMBER: 30
fun case_30() {
    val a = In<Number?>()
    val b = a.getWithUpperBoundT<Int?>()

    if (b != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
    }
}

// TESTCASE NUMBER: 31
fun case_31(y: Inv<Int?>) {
    val x = y.get()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 32
fun case_32(y: Inv<Int>) {
    val x = y.getNullable()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 33
fun case_33(y: Inv<Int>) {
    val x = y.getNullable()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 34
fun case_34(y: Inv<Int>) {
    val x = y.getNullable()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}
