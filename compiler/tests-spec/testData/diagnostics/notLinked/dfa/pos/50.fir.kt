// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
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
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>this<!>.prop_4
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>this<!>.prop_4.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>prop_4<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun Any.case_2() {
    if (this is ClassWithSixTypeParameters<*, *, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>")!>this<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>")!>this<!>.y
        <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(*)")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>")!>y<!>
    }
}

// TESTCASE NUMBER: 3
fun <T> T.case_3() {
    if (this is Inv<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & T & Any")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & T & Any")!>this<!>.prop_4
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & T & Any")!>this<!>.prop_4.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & T & Any"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_EXPRESSION_TYPE("Inv<*> & T & Any")!>prop_4<!>.inv()
    }
}

// TESTCASE NUMBER: 4
fun <T> T?.case_4() {
    if (this is ClassWithSixTypeParameters<*, *, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & T? & Any")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & T? & Any")!>this<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & T? & Any")!>this<!>.y
        <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(*)")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & T? & Any"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & T? & Any")!>y<!>
    }
}

// TESTCASE NUMBER: 5
fun <T> ClassWithSixTypeParameters<out T, *, T, in T?, *, T>.case_5() {
    if (this is InterfaceWithFiveTypeParameters1<*, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.itest1()
        itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.y
        <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(out T)")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>"), DEBUG_INFO_EXPRESSION_TYPE("T"), DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>y<!>
    }
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-25432
 */
fun <T> case_6(y: Inv<out T>) {
    if (y.prop_3 is MutableList<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & CapturedType(out T) & Any")!>y.prop_3<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & CapturedType(out T) & Any")!>y.prop_3<!>[0]
    }
}

/*
 * TESTCASE NUMBER: 7
 * ISSUES: KT-25432
 */
fun <T> Inv<out T>.case_7() {
    if (this.prop_3 is MutableList<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & CapturedType(out T) & Any")!>this.prop_3<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & CapturedType(out T) & Any")!>this.prop_3<!>[0]
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & CapturedType(out T) & Any")!>prop_3<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & CapturedType(out T) & Any")!>prop_3<!>[0]
    }
}

// TESTCASE NUMBER: 8
fun <T> T.case_8() {
    this <!UNCHECKED_CAST!>as MutableList<T><!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Any?, kotlin.Boolean>")!>this::equals<!>
    this.equals(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Any?, kotlin.Boolean>")!>::equals<!>
    equals(10)
}

/*
 * TESTCASE NUMBER: 9
 * ISSUES: KT-8966
 */
fun <T> T.case_9() {
    if (this is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & T & Any")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & T & Any")!>this<!>.length
        length
    }
}
