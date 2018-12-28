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
 * NUMBER: 8
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality) using if expression and generic types.
 * HELPERS: properties, classes
 */

// TESTCASE NUMBER: 1
fun case_1(x: Inv<Int>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 2
fun case_2(a: Inv<Inv<Inv<Inv<Inv<Inv<Int?>?>?>?>?>?>?) {
    if (a != null) {
        val b = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?>?> & Inv<Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?>?>?"), DEBUG_INFO_SMARTCAST!>a<!>.get()
        if (b != null) {
            val c = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?> & Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?>?"), DEBUG_INFO_SMARTCAST!>b<!>.get()
            if (c != null) {
                val d = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?> & Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?"), DEBUG_INFO_SMARTCAST!>c<!>.get()
                if (d != null) {
                    val e = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<kotlin.Int?>?>?> & Inv<Inv<Inv<kotlin.Int?>?>?>?"), DEBUG_INFO_SMARTCAST!>d<!>.get()
                    if (e != null) {
                        val f = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<kotlin.Int?>?> & Inv<Inv<kotlin.Int?>?>?"), DEBUG_INFO_SMARTCAST!>e<!>.get()
                        if (f != null) {
                            val g = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int?> & Inv<kotlin.Int?>?"), DEBUG_INFO_SMARTCAST!>f<!>.get()
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
fun case_3(a: Inv<Int>?) {
    if (a != null) {
        val b = a
        if (<!SENSELESS_COMPARISON!>a == null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: Inv<Int>?, b: Inv<Int> = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>a<!> else Inv<Int>()) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.equals(b)
}

// TESTCASE NUMBER: 5
fun case_5() {
    if (nullableOut != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?")!>nullableOut<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>nullableOut<!>.equals(nullableOut)
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    val x: Inv<Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Inv<Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: ClassWithThreeTypeParameters<Int?, Short?, ClassWithThreeTypeParameters<Int?, Short?, String?>?>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.equals(<!DEBUG_INFO_SMARTCAST!>x<!>.x)
        }
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.equals(<!DEBUG_INFO_SMARTCAST!>x<!>.y)
        }
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.z != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?")!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.equals(<!DEBUG_INFO_SMARTCAST!>x<!>.z)
            if (<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.equals(<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x)
            }
            if (<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.equals(<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.equals(<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z)
            }
        }
    }
}
