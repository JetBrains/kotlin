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
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 2
fun case_2(a: Inv<Inv<Inv<Inv<Inv<Inv<Int?>?>?>?>?>?>?) {
    if (a != null) {
        val b = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?>?>? & Inv<Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?>?>")!>a<!>.get()
        if (b != null) {
            val c = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?>? & Inv<Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>?>")!>b<!>.get()
            if (c != null) {
                val d = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>? & Inv<Inv<Inv<Inv<kotlin.Int?>?>?>?>")!>c<!>.get()
                if (d != null) {
                    val e = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<kotlin.Int?>?>?>? & Inv<Inv<Inv<kotlin.Int?>?>?>")!>d<!>.get()
                    if (e != null) {
                        val f = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<kotlin.Int?>?>? & Inv<Inv<kotlin.Int?>?>")!>e<!>.get()
                        if (f != null) {
                            val g = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int?>? & Inv<kotlin.Int?>")!>f<!>.get()
                            if (g != null) {
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.equals(null)
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.propT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.propAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.propNullableT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.propNullableAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.funT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.funAny()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.funNullableT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>g<!>.funNullableAny()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & kotlin.Nothing")!>b<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: Inv<Int>?, b: Inv<Int> = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>a<!> else Inv<Int>()) {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Int>? & Out<kotlin.Int>")!>nullableOut<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    val x: Inv<Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Inv<Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>? & Inv<kotlin.Int>")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: ClassWithThreeTypeParameters<Int?, Short?, ClassWithThreeTypeParameters<Int?, Short?, String?>?>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>? & ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, ClassWithThreeTypeParameters<kotlin.Int?, kotlin.Short?, kotlin.String?>?>")!>x<!>.funNullableAny()
        if (x.x != null) {
            x.x
            x.x.equals(null)
            x.x.propT
            x.x.propAny
            x.x.propNullableT
            x.x.propNullableAny
            x.x.funT()
            x.x.funAny()
            x.x.funNullableT()
            x.x.funNullableAny()
        }
        if (x.y != null) {
            x.y
            x.y.equals(null)
            x.y.propT
            x.y.propAny
            x.y.propNullableT
            x.y.propNullableAny
            x.y.funT()
            x.y.funAny()
            x.y.funNullableT()
            x.y.funNullableAny()
        }
        if (x.z != null) {
            x.z
            x.z.equals(null)
            x.z.propT
            x.z.propAny
            x.z.propNullableT
            x.z.propNullableAny
            x.z.funT()
            x.z.funAny()
            x.z.funNullableT()
            x.z.funNullableAny()
            if (x.z.x != null) {
                x.z.x
            x.z.x.equals(null)
            x.z.x.propT
            x.z.x.propAny
            x.z.x.propNullableT
            x.z.x.propNullableAny
            x.z.x.funT()
            x.z.x.funAny()
            x.z.x.funNullableT()
            x.z.x.funNullableAny()
            }
            if (x.z.y != null && x.z.z != null) {
                x.z.y
                x.z.y.equals(null)
                x.z.y.propT
                x.z.y.propAny
                x.z.y.propNullableT
                x.z.y.propNullableAny
                x.z.y.funT()
                x.z.y.funAny()
                x.z.y.funNullableT()
                x.z.y.funNullableAny()
                x.z.z
                x.z.z.equals(null)
                x.z.z.propT
                x.z.z.propAny
                x.z.z.propNullableT
                x.z.z.propNullableAny
                x.z.z.funT()
                x.z.z.funAny()
                x.z.z.funNullableT()
                x.z.z.funNullableAny()
            }
        }
    }
}
