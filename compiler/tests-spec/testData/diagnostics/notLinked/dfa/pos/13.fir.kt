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

    if (y != x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.apply { funNullableAny(); <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny() }
    }
}

// TESTCASE NUMBER: 2
fun <T> case_2(x: T?, y: Nothing?) {
    if (y != x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.apply { funNullableAny(); <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.equals(null) }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propNullableT }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propNullableAny }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funAny() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funNullableT() }
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funNullableAny() }
    }
}

// TESTCASE NUMBER: 3
fun <T> case_3(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun <T> case_4(x: T?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun <T> case_5(x: T?) {
    if (x is Interface1) {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.equals(null)
            x.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableAny
            x.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.itest()

            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.equals(null)

            x.propT

            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propAny

            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableT

            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableAny

            x.funT()

            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funAny()

            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableT()

            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.itest()
            x.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>
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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.itest()
            }
            x.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 6
fun <T> case_6(x: T?) {
    if (x is Interface1?) {
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.equals(null)
            x.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableAny
            x.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.itest()

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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>
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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.itest()
            }
            x.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 7
fun <T> case_7(y: T) {
    val x = y
    if (x is Interface1?) {
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.equals(null)
            x.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.propNullableAny
            x.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.itest()

            x.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>
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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.itest()
            }
            x.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 8
fun <T> case_8(x: T) {
    if (x != null) {
        if (x is Interface1?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.equals(null)
            x.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.propNullableAny
            x.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.itest()

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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>
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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.itest()
            }
            x.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.funNullableAny()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.toByte()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.toByte()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        x.also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.equals(null)
            x.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.propNullableAny
            x.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>x<!>.itest()

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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>
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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.itest()
            }
            x.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun <T> case_12(x: T) where T : Number?, T: Interface1? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.toByte()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest()
        }
        x.also {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.get()
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
        x.get()
        x.compareTo(null)
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            get()
            compareTo(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            this.compareTo(null)
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.get()
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
 * ISSUES: KT-28785
 */
fun <T: Out<*>?> case_14(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.get()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.get()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
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
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithFiveTypeParameters1<*, *, *, *, *>?> case_15(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.itest()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest()
        }
        x.also {
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
 * TESTCASE NUMBER: 16
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_16(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
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
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> case_17(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
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
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> case_18(x: T) {
    val y = x

    if (y != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
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
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_19(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
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
 * ISSUES: KT-28785
 */
fun <T> case_20(x: T) where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test2()

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
        x.ip1test2()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            ip1test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test2()
        }
        x.also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test2()
        }
    }
}

/*
 * TESTCASE NUMBER: 21
 * ISSUES: KT-28785
 */
fun <T> case_21(x: T) where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>?, T: InterfaceWithTypeParameter3<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test3()

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
        x.ip1test2()
        x.ip1test3()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            ip1test2()
            ip1test3()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test3()
        }
        x.also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test2()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
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

// TESTCASE NUMBER: 23
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> case_23(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()
        }
        x.also {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()
        }
        x.also {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>x<!>.isEmpty()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 33
fun <T> case_33(x: InterfaceWithFiveTypeParameters1<T, *, T, *, T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>x<!>.itest()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.itest()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>it<!>.itest()
        }
    }
}

// TESTCASE NUMBER: 34
fun <T> case_34(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>x<!>.isEmpty()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 38
fun <T> case_38(x: Map<*, <!REDUNDANT_PROJECTION!>out<!> T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>x<!>.isEmpty()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>this<!>.isEmpty()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 39
fun <T> case_39(x: InterfaceWithTwoTypeParameters<in T, out T>?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>x<!>.funNullableAny()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>x<!>.funNullableAny()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>x<!>.isEmpty()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.isEmpty()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>x<!>.isEmpty()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.isEmpty()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>x<!>.isEmpty()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.isEmpty()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>x<!>.itest()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.itest()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>it<!>.itest()
        }
    }
}

// TESTCASE NUMBER: 45
fun <T> case_45(x: T) where T : Number?, T: Comparable<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.toByte()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.compareTo(x)

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
        x.compareTo(x)
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            x.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(x)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        x.also {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.compareTo(x)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.get(0)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.iterator()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.compareTo(x)
        x.get(0)
        x.iterator()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(x)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get(0)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.iterator()
        }
        x.also {
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
 * ISSUES: KT-28785
 */
fun <T> case_47(x: T?) where T : Inv<T>, T: Comparable<*>?, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.compareTo(return)
        x.compareTo(return)

        x.apply {
            compareTo(return)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.compareTo(return)
        }

        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.compareTo(return)
        }
    }
}

/*
 * TESTCASE NUMBER: 48
 * ISSUES: KT-28785
 */
fun <T> case_48(x: T?) where T : Inv<out T>, T: InterfaceWithTypeParameter1<in T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 49
 * ISSUES: KT-28785
 */
fun <T> case_49(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<in T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 50
 * ISSUES: KT-28785
 */
fun <T> case_50(x: T?) where T : Inv<out T>, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 51
 * ISSUES: KT-28785
 */
fun <T> case_51(x: T?) where T : Inv<T>, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 52
fun <T> case_52(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 53
 * ISSUES: KT-28785
 */
fun <T> case_53(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<*>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 54
 * ISSUES: KT-28785
 */
fun <T> case_54(x: T?) where T : Inv<*>, T: InterfaceWithTypeParameter1<out T?>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 55
fun <T> case_55(x: T?) where T : Inv<*>, T: InterfaceWithTypeParameter1<T>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.test()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 56
fun <T> case_56(x: T) where T : Number?, T: Interface1? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.toByte()

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
        x.toByte()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            x.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        x.also {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.get()
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
        x.get()
        x.compareTo(null)
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            get()
            compareTo(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.compareTo(null)
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.compareTo(null)
        }
    }
}

// TESTCASE NUMBER: 58
fun <T : InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<T>>>>>>>>>>?> case_59(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
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
 * ISSUES: KT-28785
 */
fun <T> case_59(x: T) where T: InterfaceWithFiveTypeParameters1<in T, *, out T?, Nothing?, T>?, T: InterfaceWithFiveTypeParameters2<out T, in T?, T, *, Unit?>?, T: InterfaceWithFiveTypeParameters3<out Nothing, in T, T, in Int?, Number>? {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.itest2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.itest3()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest1()
        x.itest2()
        x.itest3()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest1()
            itest2()
            itest3()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest3()
        }
        x.also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest3()
        }
    }
}

/*
 * TESTCASE NUMBER: 60
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_60(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()

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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        x.also {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.test4()

        x.ip1test1()
        x.test2()
        x.ip1test1()
        x.test4()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            ip1test1()
            test2()
            ip1test1()
            test4()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test4()
        }
        x.also {
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
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.hashCode()

        x.hashCode()
        x.apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            hashCode()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.hashCode()
        }
        x.also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.hashCode()
        }
    }
}
