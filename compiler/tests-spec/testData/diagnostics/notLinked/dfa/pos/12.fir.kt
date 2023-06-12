// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 12
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, interfaces, properties, functions
 */

// TESTCASE NUMBER: 1
fun <T> T.case_1() {
    if (this != null) {
        equals(this)
        apply { equals(null) }
        apply { propT }
        apply { propAny }
        apply { propNullableT }
        apply { propNullableAny }
        apply { funT() }
        apply { funAny() }
        apply { funNullableT() }
        apply { funNullableAny(); <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(null) }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(null) }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propT }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propAny }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableT }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.propNullableAny }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funT() }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funAny() }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableT() }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.funNullableAny() }
    }
}

// TESTCASE NUMBER: 2
fun <T> T?.case_2() {
    if (this != null) {
        equals(this)
        apply { equals(null) }
        apply { propT }
        apply { propAny }
        apply { propNullableT }
        apply { propNullableAny }
        apply { funT() }
        apply { funAny() }
        apply { funNullableT() }
        apply { funNullableAny(); <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>this<!>.equals(null) }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.equals(null) }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propT }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propAny }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propNullableT }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.propNullableAny }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funT() }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funAny() }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funNullableT() }
        also { <!DEBUG_INFO_EXPRESSION_TYPE("T?!!")!>it<!>.funNullableAny() }
    }
}

// TESTCASE NUMBER: 3
fun <T> T.case_3() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun <T> T?.case_4() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun <T> T?.case_5() {
    if (this is Interface1) {
        if (<!SENSELESS_COMPARISON!>this != null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.equals(null)
            this.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propNullableAny
            this.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.itest1()

            equals(this)
            itest1()
            apply {
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
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.itest1()
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
fun <T> T?.case_6() {
    if (this is Interface1?) {
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.equals(null)
            this.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propNullableAny
            this.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.itest1()

            equals(this)
            itest1()
            apply {
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
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T?!!")!>it<!>.itest1()
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
fun <T> T.case_7() {
    val x = this
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>x<!>.itest1()

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
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.itest1()
            }
            x.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.itest1()
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
fun <T> T.case_8() {
    if (this != null) {
        if (this is Interface1?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.equals(null)
            this.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.propNullableAny
            this.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & T!!")!>this<!>.itest1()

            equals(this)
            itest1()
            apply {
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
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!!")!>it<!>.itest1()
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
fun <T : Number> T.case_9() {
    if (<!SENSELESS_COMPARISON!>this != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
        apply {
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
            toByte()
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
        also {
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
fun <T : Number?> T.case_10() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.toByte()

        equals(this)
        toByte()
        apply {
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
        also {
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
fun <T : Number> T?.case_11() {
    if (this is Interface1?) {
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.equals(null)
            this.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.propNullableAny
            this.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.funNullableAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("T? & Interface1 & T?!!")!>this<!>.itest1()

            equals(this)
            itest1()
            apply {
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
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.funNullableAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T")!>it<!>.itest1()
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
fun <T> T.case_12() where T : Number?, T: Interface1? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.toByte()

        equals(this)
        itest1()
        apply {
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
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest1()
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
fun <T> T.case_13() where T : Out<*>?, T: Comparable<T?> {
    if (<!SENSELESS_COMPARISON!>this != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: Out<*>?> T.case_14() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.get()

        equals(this)
        get()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithFiveTypeParameters1<*, *, *, *, *>?> T.case_15() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.itest1()

        equals(this)
        itest1()
        apply {
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
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest1()
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
fun <T: InterfaceWithTypeParameter1<out T>?> T.case_16() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> T.case_17() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> T.case_18() {
    val y = this

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

        equals(y)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> T.case_19() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_20() where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test2()

        equals(this)
        ip1test1()
        ip1test2()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_21() where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>?, T: InterfaceWithTypeParameter3<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test3()

        equals(this)
        ip1test1()
        ip1test2()
        ip1test3()
        apply {
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
        also {
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
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> T.case_22() {
    var y = this

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

        equals(y)
        ip1test1()
        apply {
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
        also {
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
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> T.case_23() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<T>?.case_24() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<T>?.case_25() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>? & InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<T>> InterfaceWithTypeParameter1<in T>?.case_26() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<T>> InterfaceWithTypeParameter1<out T>?.case_27() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<out T>?.case_28() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<in T>?.case_29() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<in T>?.case_30() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<out T>?.case_31() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T> Map<T, *>?.case_32() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>? & kotlin.collections.Map<T, *>")!>this<!>.isEmpty()

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
        apply {
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
        also {
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
fun <T> InterfaceWithFiveTypeParameters1<T, *, T, *, T>?.case_33() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>? & InterfaceWithFiveTypeParameters1<T, *, T, *, T>")!>this<!>.itest()

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
        apply {
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
        also {
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
fun <T> InterfaceWithTypeParameter1<out T>?.case_34() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T> InterfaceWithTypeParameter1<in T>?.case_35() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>? & InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T> InterfaceWithTypeParameter1<out T>?.case_36() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>? & InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()

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
        apply {
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
        also {
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
fun <T> Map<in T, *>?.case_37() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>? & kotlin.collections.Map<in T, *>")!>this<!>.isEmpty()

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
        apply {
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
        also {
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
fun <T> Map<*, out T>?.case_38() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>? & kotlin.collections.Map<*, out T>")!>this<!>.isEmpty()

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
        apply {
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
        also {
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
fun <T> InterfaceWithTwoTypeParameters<in T, out T>?.case_39() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>? & InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.funNullableAny()

        equals(null)

        propT

        propAny

        propNullableT

        propNullableAny

        funT()

        funAny()

        funNullableT()

        funNullableAny()
        apply {
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
        also {
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
fun <T> InterfaceWithTwoTypeParameters<in T, in T>?.case_40() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>? & InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.funNullableAny()

        equals(null)

        propT

        propAny

        propNullableT

        propNullableAny

        funT()

        funAny()

        funNullableT()

        funNullableAny()
        apply {
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
        also {
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
fun <T> Map<out T, out T>?.case_41() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>? & kotlin.collections.Map<out T, out T>")!>this<!>.isEmpty()

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
        apply {
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
        also {
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
fun <T> Map<T, out T>?.case_42() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>? & kotlin.collections.Map<T, out T>")!>this<!>.isEmpty()

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
        apply {
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
        also {
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
fun <T> Map<in T, T>?.case_43() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>? & kotlin.collections.Map<in T, T>")!>this<!>.isEmpty()

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
        apply {
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
        also {
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
fun <T> InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?.case_44() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>? & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>")!>this<!>.itest()

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
        apply {
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
        also {
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
fun <T> T.case_45() where T : Number?, T: Comparable<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.toByte()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.compareTo(this)

        equals(this)
        toByte()
        compareTo(this)
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        also {
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
fun <T> T.case_46() where T : CharSequence?, T: Comparable<T>?, T: Iterable<*>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.compareTo(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.get(0)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.iterator()

        equals(this)
        compareTo(this)
        get(0)
        iterator()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get(0)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.iterator()
        }
        also {
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
fun <T> T?.case_47() where T : Inv<T>, T: Comparable<*>?, T: InterfaceWithTypeParameter1<out T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.compareTo(return)
        compareTo(return)

        apply {
            compareTo(return)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.compareTo(return)
        }

        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.compareTo(return)
        }
    }
}

/*
 * TESTCASE NUMBER: 48
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_48() where T : Inv<out T>, T: InterfaceWithTypeParameter1<in T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 49
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_49() where T : Inv<in T>, T: InterfaceWithTypeParameter1<in T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 50
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_50() where T : Inv<out T>, T: InterfaceWithTypeParameter1<out T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 51
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_51() where T : Inv<T>, T: InterfaceWithTypeParameter1<out T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 52
fun <T> T?.case_52() where T : Inv<in T>, T: InterfaceWithTypeParameter1<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 53
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_53() where T : Inv<in T>, T: InterfaceWithTypeParameter1<*>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 54
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_54() where T : Inv<*>, T: InterfaceWithTypeParameter1<out T?>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 55
fun <T> T?.case_55() where T : Inv<*>, T: InterfaceWithTypeParameter1<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>this<!>.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.ip1test1()
        }
        also {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 56
fun <T> T.case_56() where T : Number?, T: Interface1? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.toByte()

        equals(this)
        itest()
        toByte()
        apply {
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
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        also {
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
fun <T> T.case_57() where T : Out<*>?, T: Comparable<T?> {
    if (<!SENSELESS_COMPARISON!>this != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<T>>>>>>>>>>?> T.case_59() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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
fun <T> T.case_59() where T: InterfaceWithFiveTypeParameters1<in T, *, out T?, Nothing?, T>?, T: InterfaceWithFiveTypeParameters2<out T, in T?, T, *, Unit?>?, T: InterfaceWithFiveTypeParameters3<out Nothing, in T, T, in Int?, Number>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.itest2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.itest3()

        equals(this)
        itest1()
        itest2()
        itest3()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> T.case_60() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
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
        also {
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

fun <T> T.case_61() where T : InterfaceWithTypeParameter1<T>?, T: Case61_3<T>?, T: Case61_1<T>?, T: Case61_2<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.test4()

        test1()
        test2()
        ip1test1()
        test4()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            test1()
            test2()
            ip1test1()
            test4()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test4()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.test4()
        }
    }
}

// TESTCASE NUMBER: 62
fun Nothing?.case_62() {
    if (<!SENSELESS_COMPARISON!>this != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>this<!>.hashCode()

        hashCode()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>
            hashCode()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>.hashCode()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!>.hashCode()
        }
    }
}

// TESTCASE NUMBER: 63
fun Nothing.case_63() {
    if (<!SENSELESS_COMPARISON!>this != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>.hashCode()

        hashCode()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>
            hashCode()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>.hashCode()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!>.hashCode()
        }
    }
}

/*
 * TESTCASE NUMBER: 64
 * UNEXPECTED BEHAVIOUR
 */
fun <T : Nothing?> T.case_64() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>this<!>.hashCode()

        hashCode()
        apply {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
        hashCode()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.hashCode()
    }
        also {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.hashCode()
    }
    }
}

/*
 * TESTCASE NUMBER: 65
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_65() {
    if (this is Interface1?) {
        if (this is Interface2?) {
            if (this != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>.equals(null)
                this.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>.propNullableAny
                this.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("T & Interface1 & Interface2 & T!!")!>this<!>.funNullableAny()
                apply {
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.equals(null)
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.propT
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.propAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.propNullableT
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.propNullableAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.funT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.funAny()
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.funNullableT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>this<!>.funNullableAny()
                }
                also {
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.equals(null)
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.propT
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.propAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.propNullableT
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.propNullableAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.funT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.funAny()
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.funNullableT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T!!")!>it<!>.funNullableAny()
                }
            }
        }
    }
}
