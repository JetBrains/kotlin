// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 13
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, interfaces, properties, functions
 */

// TESTCASE NUMBER: 1
fun <T> case_1(x: T) {
    var y = null

    if (<!DEBUG_INFO_CONSTANT!>y<!> != x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funNullableAny(); <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny() }
    }
}

// TESTCASE NUMBER: 2
fun <T> case_2(x: T?, y: Nothing?) {
    if (<!DEBUG_INFO_CONSTANT!>y<!> != x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.apply { funNullableAny(); <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny() }
    }
}

// TESTCASE NUMBER: 3
fun <T> case_3(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun <T> case_4(x: T?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun <T> case_5(x: T?) {
    if (x is Interface1) {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableAny
            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.itest()

            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

            <!DEBUG_INFO_SMARTCAST!>x<!>.propT

            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny

            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableT

            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableAny

            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()

            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableT()

            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.itest()
            <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.itest()
            }
            <!DEBUG_INFO_SMARTCAST!>x<!>.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 6
fun <T> case_6(x: T?) {
    if (x is Interface1?) {
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableAny
            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.itest()

            <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

            <!DEBUG_INFO_SMARTCAST!>x<!>.propT

            <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

            x.propNullableT

            x.propNullableAny

            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

            <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

            x.funNullableT()

            x.funNullableAny()
            <!DEBUG_INFO_SMARTCAST!>x<!>.itest()
            <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.itest()
            }
            <!DEBUG_INFO_SMARTCAST!>x<!>.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 7
fun <T> case_7(y: T) {
    val x = y
    if (x is Interface1?) {
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.propNullableAny
            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.itest()

            <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.itest()
            }
            <!DEBUG_INFO_SMARTCAST!>x<!>.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 8
fun <T> case_8(x: T) {
    if (x != null) {
        if (x is Interface1?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.propNullableAny
            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.itest()

            <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

            <!DEBUG_INFO_SMARTCAST!>x<!>.propT

            <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

            x.propNullableT

            x.propNullableAny

            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

            <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

            x.funNullableT()

            x.funNullableAny()
            <!DEBUG_INFO_SMARTCAST!>x<!>.itest()
            <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.itest()
            }
            <!DEBUG_INFO_SMARTCAST!>x<!>.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 9
fun <T : Number> case_9(x: T) {
    if (<!SENSELESS_COMPARISON!>x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.toByte()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.toByte()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            x.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.toByte()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.toByte()
        }
    }
}

// TESTCASE NUMBER: 10
fun <T : Number?> case_10(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.toByte()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.toByte()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 11
fun <T : Number> case_11(x: T?) {
    if (x is Interface1?) {
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.propNullableAny
            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?"), DEBUG_INFO_SMARTCAST!>x<!>.itest()

            <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

            <!DEBUG_INFO_SMARTCAST!>x<!>.propT

            <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

            x.propNullableT

            x.propNullableAny

            <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

            <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

            x.funNullableT()

            x.funNullableAny()
            <!DEBUG_INFO_SMARTCAST!>x<!>.itest()
            <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.itest()
            }
            <!DEBUG_INFO_SMARTCAST!>x<!>.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun <T> case_12(x: T) where T : Number?, T: Interface1? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.toByte()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.itest()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_13(x: T) where T : Out<*>?, T: Comparable<T?> {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!><!UNSAFE_CALL!>.<!>get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.compareTo(null)

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>get()
        x.compareTo(null)
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>get<!>()
            compareTo(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>get()
            this.compareTo(null)
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableAny()
            it.compareTo(null)
        }
    }
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: Out<*>?> case_14(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>get()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>get()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>get<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>get()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 15
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithFiveTypeParameters1<*, *, *, *, *>?> case_15(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>itest()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>itest()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>itest<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 16
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_16(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> case_17(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> case_18(x: T) {
    val y = x

    if (y != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 19
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_19(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 20
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_20(x: T) where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test2()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        x<!UNSAFE_CALL!>.<!>ip1test2()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!UNSAFE_CALL!>ip1test2<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test2()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test2()
        }
    }
}

/*
 * TESTCASE NUMBER: 21
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_21(x: T) where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>?, T: InterfaceWithTypeParameter3<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test3()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        x<!UNSAFE_CALL!>.<!>ip1test2()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test3()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!UNSAFE_CALL!>ip1test2<!>()
            ip1test3()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test3()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test3()
        }
    }
}

/*
 * TESTCASE NUMBER: 22
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> case_22(x: T) {
    var y = x

    if (y != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.ip1test1()

        x<!UNSAFE_CALL!>.<!>equals(null)

        x.propT

        x<!UNSAFE_CALL!>.<!>propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x<!UNSAFE_CALL!>.<!>funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            <!UNSAFE_CALL!>equals<!>(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

// TESTCASE NUMBER: 23
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> case_23(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 24
fun <T : InterfaceWithTypeParameter1<in T>> case_24(x: InterfaceWithTypeParameter1<T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 25
fun <T : InterfaceWithTypeParameter1<out T>> case_25(x: InterfaceWithTypeParameter1<T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 26
fun <T : InterfaceWithTypeParameter1<T>> case_26(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 27
fun <T : InterfaceWithTypeParameter1<T>> case_27(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 28
fun <T : InterfaceWithTypeParameter1<in T>> case_28(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 29
fun <T : InterfaceWithTypeParameter1<out T>> case_29(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 30
fun <T : InterfaceWithTypeParameter1<in T>> case_30(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 31
fun <T : InterfaceWithTypeParameter1<out T>> case_31(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 32
fun <T> case_32(x: Map<T, *>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?")!>x<!>.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 33
fun <T> case_33(x: InterfaceWithFiveTypeParameters1<T, *, T, *, T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>x<!>.itest()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.itest()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.itest()
        }
    }
}

// TESTCASE NUMBER: 34
fun <T> case_34(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 35
fun <T> case_35(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 36
fun <T> case_36(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 37
fun <T> case_37(x: Map<in T, *>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?")!>x<!>.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 38
fun <T> case_38(x: Map<*, <!REDUNDANT_PROJECTION!>out<!> T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?")!>x<!>.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 39
fun <T> case_39(x: InterfaceWithTwoTypeParameters<in T, out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?")!>x<!>.funNullableAny()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funNullableAny()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 40
fun <T> case_40(x: InterfaceWithTwoTypeParameters<in T, in T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?")!>x<!>.funNullableAny()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funNullableAny()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 41
fun <T> case_41(x: Map<out T, <!REDUNDANT_PROJECTION!>out<!> T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?")!>x<!>.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 42
fun <T> case_42(x: Map<T, <!REDUNDANT_PROJECTION!>out<!> T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?")!>x<!>.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 43
fun <T> case_43(x: Map<in T, T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?")!>x<!>.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 44
fun <T> case_44(x: InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>x<!>.itest()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.itest()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.itest()
        }
    }
}

// TESTCASE NUMBER: 45
fun <T> case_45(x: T) where T : Number?, T: Comparable<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.toByte()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.compareTo(x)

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.toByte()
        <!DEBUG_INFO_SMARTCAST!>x<!>.compareTo(x)
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            compareTo(this)
            <!DEBUG_INFO_SMARTCAST!>x<!>.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(x)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.compareTo(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.toByte()
        }
    }
}

// TESTCASE NUMBER: 46
fun <T> case_46(x: T) where T : CharSequence?, T: Comparable<T>?, T: Iterable<*>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.compareTo(x)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.iterator()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.compareTo(x)
        <!DEBUG_INFO_SMARTCAST!>x<!>.get(0)
        <!DEBUG_INFO_SMARTCAST!>x<!>.iterator()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            compareTo(this)
            get(0)
            iterator()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(x)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get(0)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.iterator()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.compareTo(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get(0)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.iterator()
        }
    }
}

/*
 * TESTCASE NUMBER: 47
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_47(x: T?) where T : Inv<T>, T: Comparable<*>?, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!><!UNREACHABLE_CODE!>compareTo(<!>return<!UNREACHABLE_CODE!>)<!>
        <!UNREACHABLE_CODE!>x<!UNSAFE_CALL!>.<!>compareTo(return)<!>

        <!UNREACHABLE_CODE!><!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!UNREACHABLE_CODE!><!UNSAFE_CALL!>compareTo<!>(<!>return<!UNREACHABLE_CODE!>)<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>compareTo(return)<!>
        }<!>

        <!UNREACHABLE_CODE!><!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!><!UNREACHABLE_CODE!>compareTo(<!>return<!UNREACHABLE_CODE!>)<!>
        }<!>
    }
}

/*
 * TESTCASE NUMBER: 48
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_48(x: T?) where T : Inv<out T>, T: InterfaceWithTypeParameter1<in T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 49
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_49(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<in T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 50
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_50(x: T?) where T : Inv<out T>, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 51
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_51(x: T?) where T : Inv<T>, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

// TESTCASE NUMBER: 52
fun <T> case_52(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 53
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_53(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<*>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 54
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_54(x: T?) where T : Inv<*>, T: InterfaceWithTypeParameter1<out T?>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

// TESTCASE NUMBER: 55
fun <T> case_55(x: T?) where T : Inv<*>, T: InterfaceWithTypeParameter1<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 56
fun <T> case_56(x: T) where T : Number?, T: Interface1? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.toByte()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.itest()
        <!DEBUG_INFO_SMARTCAST!>x<!>.toByte()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            <!DEBUG_INFO_SMARTCAST!>x<!>.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.toByte()
        }
    }
}

/*
 * TESTCASE NUMBER: 57
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_57(x: T) where T : Out<*>?, T: Comparable<T?> {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!><!UNSAFE_CALL!>.<!>get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.compareTo(null)

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>get()
        x.compareTo(null)
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>get<!>()
            compareTo(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.compareTo(null)
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.compareTo(null)
        }
    }
}

// TESTCASE NUMBER: 58
fun <T : InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<T>>>>>>>>>>?> case_59(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 59
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_59(x: T) where T: InterfaceWithFiveTypeParameters1<in T, *, out T?, Nothing?, T>?, T: InterfaceWithFiveTypeParameters2<out T, in T?, T, *, Unit?>?, T: InterfaceWithFiveTypeParameters3<out Nothing, in T, T, in Int?, Number>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>itest2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>itest3()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>itest1()
        x<!UNSAFE_CALL!>.<!>itest2()
        x<!UNSAFE_CALL!>.<!>itest3()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>itest1<!>()
            <!UNSAFE_CALL!>itest2<!>()
            <!UNSAFE_CALL!>itest3<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest3()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>itest2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>itest3()
        }
    }
}

/*
 * TESTCASE NUMBER: 60
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_60(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_SMARTCAST!>x<!>.equals(null)

        <!DEBUG_INFO_SMARTCAST!>x<!>.propT

        <!DEBUG_INFO_SMARTCAST!>x<!>.propAny

        x.propNullableT

        x.propNullableAny

        <!DEBUG_INFO_SMARTCAST!>x<!>.funT()

        <!DEBUG_INFO_SMARTCAST!>x<!>.funAny()

        x.funNullableT()

        x.funNullableAny()
        x<!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

// TESTCASE NUMBER: 61
interface Case61_1<T>: InterfaceWithTypeParameter1<T>, Case61_2<T> { fun test1() }
interface Case61_2<T>: InterfaceWithTypeParameter1<T> { fun test2() }

class Case61_3<T>: InterfaceWithTypeParameter1<T>, Case61_1<T>, Case61_2<T> {
    override fun test1() {}
    override fun test2() {}
    fun test4() {}
}

fun <T> T.case_61(x: T) where T : InterfaceWithTypeParameter1<T>?, T: Case61_3<T>?, T: Case61_1<T>?, T: Case61_2<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.test4()

        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test2()
        <!DEBUG_INFO_SMARTCAST!>x<!>.ip1test1()
        <!DEBUG_INFO_SMARTCAST!>x<!>.test4()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            ip1test1()
            test2()
            ip1test1()
            test4()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test4()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test4()
        }
    }
}

/*
 * TESTCASE NUMBER: 62
 * UNEXPECTED BEHAVIOUR
 */
fun <T : Nothing?> case_62(x: T) {
    if (x <!EQUALS_MISSING, UNRESOLVED_REFERENCE!>!=<!> null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.hashCode()

        x.hashCode()
        <!DEBUG_INFO_SMARTCAST!>x<!>.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            hashCode()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.hashCode()
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.hashCode()
        }
    }
}