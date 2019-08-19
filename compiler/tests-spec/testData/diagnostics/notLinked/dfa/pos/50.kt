// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 50
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun Any.case_1() {
    if (this is Inv<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!><!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this<!>.prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!><!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this<!>.prop_4<!>.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>prop_4<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun Any.case_2() {
    if (this is ClassWithSixTypeParameters<*, *, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this<!>.y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>y<!>
    }
}

// TESTCASE NUMBER: 3
fun <T> T.case_3() {
    if (this is Inv<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!><!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!><!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_SMARTCAST!>this<!>.prop_4<!>.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>prop_4<!>.inv()
    }
}

// TESTCASE NUMBER: 4
fun <T> T?.case_4() {
    if (this is ClassWithSixTypeParameters<*, *, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>y<!>
    }
}

// TESTCASE NUMBER: 5
fun <T> ClassWithSixTypeParameters<out T, *, T, in T?, *, T>.case_5() {
    if (this is InterfaceWithFiveTypeParameters1<*, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.itest1()
        itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>y<!>
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25432
 */
fun <T> case_6(y: Inv<out T>) {
    if (y.prop_3 is MutableList<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>y.prop_3<!>
        <!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!><!DEBUG_INFO_EXPRESSION_TYPE("T")!>y.prop_3<!><!NO_GET_METHOD!>[0]<!><!>
    }
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25432
 */
fun <T> Inv<out T>.case_7() {
    if (this.prop_3 is MutableList<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>this.prop_3<!>
        <!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!><!DEBUG_INFO_EXPRESSION_TYPE("T")!>this.prop_3<!><!NO_GET_METHOD!>[0]<!><!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>prop_3<!>
        <!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!><!DEBUG_INFO_EXPRESSION_TYPE("T")!>prop_3<!><!NO_GET_METHOD!>[0]<!><!>
    }
}

// TESTCASE NUMBER: 8
fun <T> T.case_8() {
    this <!UNCHECKED_CAST!>as MutableList<T><!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@kotlin.ParameterName kotlin.Any?, kotlin.Boolean>")!><!DEBUG_INFO_SMARTCAST!>this<!>::equals<!>
    <!DEBUG_INFO_SMARTCAST!>this<!>.equals(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@kotlin.ParameterName kotlin.Any?, kotlin.Boolean>")!>::<!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!><!>
    <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 9
 * ISSUES: KT-8966
 */
fun <T> T.case_9() {
    if (this is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!! & kotlin.String"), DEBUG_INFO_EXPRESSION_TYPE("T")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this<!>.length
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>length<!>
    }
}
