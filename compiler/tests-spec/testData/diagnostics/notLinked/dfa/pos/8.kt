// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 8
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: properties, classes, functions
 */

// TESTCASE NUMBER: 1
fun case_1(x: Inv<Int>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.funNullableAny()
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
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>g<!>.equals(null)
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>g<!>.propT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>g<!>.propAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>g<!>.propNullableT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>g<!>.propNullableAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>g<!>.funT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>g<!>.funAny()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>g<!>.funNullableT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>g<!>.funNullableAny()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>b<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>b<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>b<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>b<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>b<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>b<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>b<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>b<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: Inv<Int>?, b: Inv<Int> = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>a<!> else Inv<Int>()) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.equals(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.propT
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.propAny
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.propNullableT
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.propNullableAny
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.funT()
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.funAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.funNullableT()
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>.funNullableAny()
}

// TESTCASE NUMBER: 5
fun case_5() {
    if (nullableOut != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?")!>nullableOut<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>nullableOut<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>nullableOut<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>nullableOut<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?")!>nullableOut<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?")!>nullableOut<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>nullableOut<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>nullableOut<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?")!>nullableOut<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int> & Out<kotlin.Int>?")!>nullableOut<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    val x: Inv<Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Inv<Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int> & Inv<kotlin.Int>?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: ClassWithThreeTypeParameters<Int?, Short?, ClassWithThreeTypeParameters<Int?, Short?, String?>?>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>?")!>x<!>.funNullableAny()
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!>x<!>.x<!>.funNullableAny()
        }
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>.funNullableAny()
        }
        if (<!DEBUG_INFO_SMARTCAST!>x<!>.z != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?")!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?")!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?")!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?")!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?> & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?")!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.funNullableAny()
            if (<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.x<!>.funNullableAny()
            }
            if (<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short & kotlin.Short?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.y<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>.z<!>.funNullableAny()
            }
        }
    }
}
