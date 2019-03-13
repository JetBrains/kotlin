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
 * NUMBER: 12
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality) on generic receivers using if expression.
 * HELPERS: classes, interfaces
 */

// TESTCASE NUMBER: 1
fun <T> T.case_1() {
    if (this != null) {
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> { equals(this); <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this) }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(this) }
    }
}

// TESTCASE NUMBER: 2
fun <T> T?.case_2() {
    if (this != null) {
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> { equals(this); <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this) }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> { <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(this) }
    }
}

// TESTCASE NUMBER: 3
fun <T> T.case_3() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
    }
}

// TESTCASE NUMBER: 4
fun <T> T?.case_4() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
    }
}

// TESTCASE NUMBER: 5
fun <T> T?.case_5() {
    if (this is Interface1) {
        if (<!SENSELESS_COMPARISON!>this != null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.itest1()

            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>itest1<!>()
            apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                equals(this)
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.equals(this)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.equals(it)
            }
        }
    }
}

// TESTCASE NUMBER: 6
fun <T> T?.case_6() {
    if (this is Interface1?) {
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.itest1()

            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>itest1<!>()
            apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                equals(this)
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.equals(this)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.equals(it)
            }
        }
    }
}

// TESTCASE NUMBER: 7
fun <T> T.case_7() {
    val x = this
    if (x is Interface1?) {
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T"), DEBUG_INFO_SMARTCAST!>x<!>.itest1()

            x.apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>
                equals(this)
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.equals(this)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.itest1()
            }
            x.also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.equals(it)
            }
        }
    }
}

// TESTCASE NUMBER: 8
fun <T> T.case_8() {
    if (this != null) {
        if (this is Interface1?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.itest1()

            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>itest1<!>()
            apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>
                equals(this)
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.equals(this)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T!!}")!>it<!>.equals(it)
            }
        }
    }
}

// TESTCASE NUMBER: 9
fun <T : Number> T.case_9() {
    if (<!SENSELESS_COMPARISON!>this != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.toByte()

        equals(this)
        toByte()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(this)
            toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.toByte()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.toByte()
        }
    }
}

// TESTCASE NUMBER: 10
fun <T : Number?> T.case_10() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.toByte()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>toByte<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
        }
    }
}

// TESTCASE NUMBER: 11
fun <T : Number> T?.case_11() {
    if (this is Interface1?) {
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.itest1()

            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>itest1<!>()
            apply {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>
                equals(this)
                itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.equals(this)
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>this<!>.itest1()
            }
            also {
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & T?!!}")!>it<!>.equals(it)
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun <T> T.case_12() where T : Number?, T: Interface1? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.toByte()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>itest1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
        }
    }
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_13() where T : Out<*>?, T: Comparable<T?> {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.compareTo(null)

        equals(this)
        <!UNSAFE_CALL!>get<!>()
        compareTo(null)
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>get<!>()
            compareTo(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>get()
            this.compareTo(null)
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>get()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>get<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>get<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>get()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>itest1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>itest1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>itest1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!UNSAFE_CALL!>equals<!>(y)
        <!UNSAFE_CALL!>ip1test1<!>()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            <!UNSAFE_CALL!>equals<!>(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>equals(y)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test2()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>ip1test1<!>()
        <!UNSAFE_CALL!>ip1test2<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!UNSAFE_CALL!>ip1test2<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test2()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
fun <T> T.case_21() where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>?, T: InterfaceWithTypeParameter3<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test3()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>ip1test1<!>()
        <!UNSAFE_CALL!>ip1test2<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test3<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!UNSAFE_CALL!>ip1test2<!>()
            ip1test3()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test3()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> T.case_22() {
    var y = this

    if (y != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>y<!>.ip1test1()

        <!UNSAFE_CALL!>equals<!>(y)
        <!UNSAFE_CALL!>ip1test1<!>()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            <!UNSAFE_CALL!>equals<!>(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

// TESTCASE NUMBER: 23
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> T.case_23() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 24
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<T>?.case_24() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 25
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<T>?.case_25() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T> & InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>?"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 26
fun <T : InterfaceWithTypeParameter1<T>> InterfaceWithTypeParameter1<in T>?.case_26() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 27
fun <T : InterfaceWithTypeParameter1<T>> InterfaceWithTypeParameter1<out T>?.case_27() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 28
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<out T>?.case_28() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 29
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<in T>?.case_29() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 30
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<in T>?.case_30() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 31
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<out T>?.case_31() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 32
fun <T> Map<T, *>?.case_32() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *> & kotlin.collections.Map<T, *>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, *>?")!>this<!>.isEmpty()

        equals(this)
        isEmpty()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>
            equals(this)
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>this<!>.isEmpty()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, kotlin.Any?>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 33
fun <T> InterfaceWithFiveTypeParameters1<T, *, T, *, T>?.case_33() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T> & InterfaceWithFiveTypeParameters1<T, *, T, *, T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, *, T, *, T>?")!>this<!>.itest()

        equals(this)
        itest()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>
            equals(this)
            itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>this<!>.itest()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<T, out kotlin.Any?, T, out kotlin.Any?, T>")!>it<!>.itest()
        }
    }
}

// TESTCASE NUMBER: 34
fun <T> InterfaceWithTypeParameter1<out T>?.case_34() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 35
fun <T> InterfaceWithTypeParameter1<in T>?.case_35() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T> & InterfaceWithTypeParameter1<in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<in T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 36
fun <T> InterfaceWithTypeParameter1<out T>?.case_36() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T> & InterfaceWithTypeParameter1<out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>?")!>this<!>.ip1test1()

        equals(this)
        ip1test1()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>this<!>.ip1test1()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out T>")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 37
fun <T> Map<in T, *>?.case_37() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *> & kotlin.collections.Map<in T, *>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, *>?")!>this<!>.isEmpty()

        equals(this)
        isEmpty()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>
            equals(this)
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>this<!>.isEmpty()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, kotlin.Any?>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 38
fun <T> Map<*, <!REDUNDANT_PROJECTION!>out<!> T>?.case_38() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T> & kotlin.collections.Map<*, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<*, out T>?")!>this<!>.isEmpty()

        equals(this)
        isEmpty()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>
            equals(this)
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>this<!>.isEmpty()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Any?, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 39
fun <T> InterfaceWithTwoTypeParameters<in T, out T>?.case_39() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T> & InterfaceWithTwoTypeParameters<in T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>?")!>this<!>.equals(this)

        equals(this)
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>
            equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>this<!>.equals(this)
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, out T>")!>it<!>.equals(it)
        }
    }
}

// TESTCASE NUMBER: 40
fun <T> InterfaceWithTwoTypeParameters<in T, in T>?.case_40() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T> & InterfaceWithTwoTypeParameters<in T, in T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>?")!>this<!>.equals(this)

        equals(this)
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>
            equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>this<!>.equals(this)
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<in T, in T>")!>it<!>.equals(it)
        }
    }
}

// TESTCASE NUMBER: 41
fun <T> Map<out T, <!REDUNDANT_PROJECTION!>out<!> T>?.case_41() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T> & kotlin.collections.Map<out T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, out T>?")!>this<!>.isEmpty()

        equals(this)
        isEmpty()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>
            equals(this)
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>this<!>.isEmpty()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out T, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 42
fun <T> Map<T, <!REDUNDANT_PROJECTION!>out<!> T>?.case_42() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T> & kotlin.collections.Map<T, out T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, out T>?")!>this<!>.isEmpty()

        equals(this)
        isEmpty()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>
            equals(this)
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>this<!>.isEmpty()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<T, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 43
fun <T> Map<in T, T>?.case_43() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T> & kotlin.collections.Map<in T, T>?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>?")!>this<!>.isEmpty()

        equals(this)
        isEmpty()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>
            equals(this)
            isEmpty()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>this<!>.isEmpty()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<in T, T>")!>it<!>.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 44
fun <T> InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?.case_44() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T> & InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?")!>this<!>.itest()

        equals(this)
        itest()
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>
            equals(this)
            itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>this<!>.itest()
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<in T, out kotlin.Any?, out T, out kotlin.Any?, T>")!>it<!>.itest()
        }
    }
}

// TESTCASE NUMBER: 45
fun <T> T.case_45() where T : Number?, T: Comparable<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.toByte()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.compareTo(this)

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>toByte<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>compareTo<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            compareTo(this)
            toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.compareTo(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.toByte()
        }
    }
}

// TESTCASE NUMBER: 46
fun <T> T.case_46() where T : CharSequence?, T: Comparable<T>?, T: Iterable<*>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.compareTo(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.get(0)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.iterator()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>compareTo<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>(0)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>iterator<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            compareTo(this)
            get(0)
            iterator()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.compareTo(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get(0)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.iterator()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!><!UNREACHABLE_CODE!>compareTo(<!>return<!UNREACHABLE_CODE!>)<!>
        <!UNREACHABLE_CODE!><!UNSAFE_CALL!>compareTo<!>(return)<!>

        <!UNREACHABLE_CODE!><!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!UNREACHABLE_CODE!><!UNSAFE_CALL!>compareTo<!>(<!>return<!UNREACHABLE_CODE!>)<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>compareTo(return)<!>
        }<!>

        <!UNREACHABLE_CODE!><!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!><!UNREACHABLE_CODE!>compareTo(<!>return<!UNREACHABLE_CODE!>)<!>
        }<!>
    }
}

/*
 * TESTCASE NUMBER: 48
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_48() where T : Inv<out T>, T: InterfaceWithTypeParameter1<in T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

// TESTCASE NUMBER: 52
fun <T> T?.case_52() where T : Inv<in T>, T: InterfaceWithTypeParameter1<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
    }
}

// TESTCASE NUMBER: 55
fun <T> T?.case_55() where T : Inv<*>, T: InterfaceWithTypeParameter1<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>get<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            get()
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 56
fun <T> T.case_56() where T : Number?, T: Interface1? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.toByte()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>itest<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>toByte<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            itest()
            toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.toByte()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>get()
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.compareTo(null)

        equals(this)
        <!UNSAFE_CALL!>get<!>()
        compareTo(null)
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>get<!>()
            compareTo(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.compareTo(null)
        }
        also {
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.equals(it)
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!><!UNSAFE_CALL!>.<!>get()
            <!DEBUG_INFO_EXPRESSION_TYPE("T")!>it<!>.compareTo(null)
        }
    }
}

// TESTCASE NUMBER: 58
fun <T : InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<T>>>>>>>>>>?> T.case_59() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>itest2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>itest3()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>itest1<!>()
        <!UNSAFE_CALL!>itest2<!>()
        <!UNSAFE_CALL!>itest3<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>itest1<!>()
            <!UNSAFE_CALL!>itest2<!>()
            <!UNSAFE_CALL!>itest3<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>itest3()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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
fun <T: InterfaceWithTypeParameter1<out T>?> T.case_60() {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(this)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(this)
        <!UNSAFE_CALL!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            equals(this)
            <!UNSAFE_CALL!>ip1test1<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!><!UNSAFE_CALL!>.<!>ip1test1()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>it<!>.equals(it)
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

fun <T> T.case_61() where T : InterfaceWithTypeParameter1<T>?, T: Case61_3<T>?, T: Case61_1<T>?, T: Case61_2<T>? {
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.test4()

        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>test2<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>ip1test1<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>test4<!>()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            test1()
            test2()
            ip1test1()
            test4()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test2()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.ip1test1()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.test4()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>this<!>.equals(this)

        equals(this)
        apply {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>this<!>
            equals(this)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>this<!>.equals(this)
        }
        also {
            <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>it<!>.equals(this)
        }
    }
}

// TESTCASE NUMBER: 63
fun Nothing.case_63() {
    if (<!SENSELESS_COMPARISON!>this <!UNREACHABLE_CODE!>!= null<!><!>) <!UNREACHABLE_CODE!>{
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>.hashCode()

        hashCode()
        <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>
            <!UNREACHABLE_CODE!>hashCode()<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>this<!>.hashCode()<!>
        }
        <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>also<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!>.hashCode()<!>
        }
    }<!>
}

/*
 * TESTCASE NUMBER: 64
 * UNEXPECTED BEHAVIOUR
 */
fun <T : Nothing?> T.case_64() {
    if (this <!EQUALS_MISSING, UNRESOLVED_REFERENCE!>!=<!> null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>.hashCode()

        hashCode()
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>apply<!> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>
            hashCode()
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T!!")!>this<!>.hashCode()
        }
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>also<!> {
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
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & T & T!!"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.equals(<!UNRESOLVED_REFERENCE!>x<!>)
                apply {
                    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2 & T!!}")!>this<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2 & T!!}"), DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2 & T!!}")!>this<!>.equals(this)
                }
                also {
                    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2 & T!!}")!>it<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("{Interface1 & Interface2 & T!!}")!>it<!>.equals(it)
                }
            }
        }
    }
}
