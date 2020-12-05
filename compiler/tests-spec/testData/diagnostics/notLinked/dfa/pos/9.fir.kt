// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(x: Out<out Int?>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int?> & Out<out kotlin.Int?>?")!>x<!>.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28598
 */
fun case_2(a: Out<out Out<out Out<out Out<out Out<out Out<out Int?>?>?>?>?>?>?) {
    if (a != null) {
        val b = <!DEBUG_INFO_EXPRESSION_TYPE("Out<out Out<out Out<out Out<out Out<out Out<out kotlin.Int?>?>?>?>?>?> & Out<out Out<out Out<out Out<out Out<out Out<out kotlin.Int?>?>?>?>?>?>?")!>a<!>.get()
        if (b != null) {
            val c = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<Out<kotlin.Any?>?>?>?>?> & Out<Out<Out<Out<Out<kotlin.Any?>?>?>?>?>?")!>b<!>.get()
            if (c != null) {
                val d = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<kotlin.Any?>?>?>?> & Out<Out<Out<Out<kotlin.Any?>?>?>?>?")!>c<!>.get()
                if (d != null) {
                    val e = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<kotlin.Any?>?>?> & Out<Out<Out<kotlin.Any?>?>?>?")!>d<!>.get()
                    if (e != null) {
                        val f = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<kotlin.Any?>?> & Out<Out<kotlin.Any?>?>?")!>e<!>.get()
                        if (f != null) {
                            val g = <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Any?> & Out<kotlin.Any?>?")!>f<!>.get()
                            if (g != null) {
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.equals(null)
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propNullableT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propNullableAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funAny()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funNullableT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funNullableAny()
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
        if (a == null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>")!>b<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: Out<out Int>?, b: Out<out Int> = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>a<!> else Out<Int>()) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.equals(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.propT
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.propAny
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.propNullableT
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.propNullableAny
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.funT()
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.funAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.funNullableT()
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int>")!>b<!>.funNullableAny()
}

// TESTCASE NUMBER: 5
val x: Out<out Int>? = null

fun case_5() {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    val x: Inv<out Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Out<out Int>? = null

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<out kotlin.Int> & Out<out kotlin.Int>?")!>x<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?> & ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, ClassWithThreeTypeParameters<out kotlin.Int?, out kotlin.Short?, out kotlin.String?>?>?")!>x<!>.funNullableAny()
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
                x.z.z
                x.z.y.equals(null)
                x.z.y.propT
                x.z.y.propAny
                x.z.y.propNullableT
                x.z.y.propNullableAny
                x.z.y.funT()
                x.z.y.funAny()
                x.z.y.funNullableT()
                x.z.y.funNullableAny()
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

// TESTCASE NUMBER: 9
fun case_9(a: (Inv<out Int>) -> Inv<out Int>?, b: Inv<out Int>?) {
    if (b != null) {
        val c = a(b)
        if (c != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Int> & Inv<out kotlin.Int>?")!>c<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 10
fun case_9(a: Inv<*>?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & Inv<*>?")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 11
fun case_10() {
    val a10: Out<*>? = null

    if (a10 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>a10<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 12
fun case_11() {
    val a: In<*>? = null

    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("In<*> & In<*>?")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 13
fun case_13(a: ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>?")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 14
fun case_14(a: ClassWithSixTypeParameters<*, Int, *, Out<*>, *, Map<Float, Out<*>>>?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>> & ClassWithSixTypeParameters<*, kotlin.Int, *, Out<*>, *, kotlin.collections.Map<kotlin.Float, Out<*>>>?")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 15
fun case_15(a: Any?) {
    if (a is ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
        if (a != null) {
            a
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 16
fun case_16(a: Any?) {
    if (a === null) {
    } else {
        if (a !is ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & kotlin.Any?")!>a<!>.funNullableAny()
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
            val c = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<Out<kotlin.Any?>?>?>?>?> & Out<Out<Out<Out<Out<kotlin.Any?>?>?>?>?>?")!>b<!>.get()
            if (c != null) {
                val d = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Out<kotlin.Any?>?>?>?> & Out<Out<Out<Out<kotlin.Any?>?>?>?>?")!>c<!>.get()
                if (d != null) {
                    val e = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<kotlin.Any?>?>?> & Out<Out<Out<kotlin.Any?>?>?>?")!>d<!>.get()
                    if (e != null) {
                        val f = <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<kotlin.Any?>?> & Out<Out<kotlin.Any?>?>?")!>e<!>.get()
                        if (f != null) {
                            val g = <!DEBUG_INFO_EXPRESSION_TYPE("Out<kotlin.Any?> & Out<kotlin.Any?>?")!>f<!>.get()
                            if (g != null) {
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.equals(null)
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propNullableT
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.propNullableAny
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funAny()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funNullableT()
                                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>g<!>.funNullableAny()
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
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 19
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_19(a: Inv<out Nothing?>) {
    if (a.x != null) {
        a.x
        a.x.hashCode()
    }
}

/*
 * TESTCASE NUMBER: 20
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_20(a: Inv<out Any?>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 21
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_21(a: Out<out Int?>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 22
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_22(a: Out<out Nothing?>) {
    if (a.x != null) {
        a.x
        a.x.hashCode()
    }
}

/*
 * TESTCASE NUMBER: 23
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_23(a: Out<out Any?>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

// TESTCASE NUMBER: 24
fun case_24(a: Out<Int?>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

// TESTCASE NUMBER: 25
fun case_25(a: Out<Nothing?>) {
    if (a.x != null) {
        a.x
        a.x.hashCode()
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: Out<Any?>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 27
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_27(a: Inv<in Any>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 28
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_28(a: Inv<in Float>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 29
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun case_29(a: Inv<in Nothing>) {
    if (a.x != null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x.propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x.funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

// TESTCASE NUMBER: 30
fun case_30() {
    val a = In<Number?>()
    val b = a.getWithUpperBoundT<Int?>(10)

    if (b != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>b<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 31
fun case_31(y: Inv<Int?>) {
    val x = y.get()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 32
fun case_32(y: Inv<Int>) {
    val x = y.getNullable()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 33
fun case_33(y: Inv<Int>) {
    val x = y.getNullable()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 34
fun case_34(y: Inv<Int>) {
    val x = y.getNullable()

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableAny()
    }
}
