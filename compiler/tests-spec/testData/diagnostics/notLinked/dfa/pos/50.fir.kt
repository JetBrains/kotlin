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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & Inv<*>")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & Inv<*>")!>this<!>.prop_4
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & Inv<*>")!>this<!>.prop_4.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>prop_4<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun Any.case_2() {
    if (this is ClassWithSixTypeParameters<*, *, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ClassWithSixTypeParameters<*, *, *, *, *, *>")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ClassWithSixTypeParameters<*, *, *, *, *, *>")!>this<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ClassWithSixTypeParameters<*, *, *, *, *, *>")!>this<!>.y
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!>
    }
}

// TESTCASE NUMBER: 3
fun <T> T.case_3() {
    if (this is Inv<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & Inv<*> & T!!")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & Inv<*> & T!!")!>this<!>.prop_4
        <!DEBUG_INFO_EXPRESSION_TYPE("T & Inv<*> & T!!")!>this<!>.prop_4.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>prop_4<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>prop_4<!>.inv()
    }
}

// TESTCASE NUMBER: 4
fun <T> T?.case_4() {
    if (this is ClassWithSixTypeParameters<*, *, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & ClassWithSixTypeParameters<*, *, *, *, *, *> & T?!!")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & ClassWithSixTypeParameters<*, *, *, *, *, *> & T?!!")!>this<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & ClassWithSixTypeParameters<*, *, *, *, *, *> & T?!!")!>this<!>.y
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!>
    }
}

// TESTCASE NUMBER: 5
fun <T> ClassWithSixTypeParameters<out T, *, T, in T?, *, T>.case_5() {
    if (this is InterfaceWithFiveTypeParameters1<*, *, *, *, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.itest1()
        itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<out T, *, T, in T?, *, T> & InterfaceWithFiveTypeParameters1<*, *, *, *, *> & ClassWithSixTypeParameters<out T, *, T, in T?, *, T>")!>this<!>.y
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>y<!>
    }
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-25432
 */
fun <T> case_6(y: Inv<out T>) {
    if (y.prop_3 is MutableList<*>) {
        y.prop_3
        y.prop_3[0]
    }
}

/*
 * TESTCASE NUMBER: 7
 * ISSUES: KT-25432
 */
fun <T> Inv<out T>.case_7() {
    if (this.prop_3 is MutableList<*>) {
        this.prop_3
        this.prop_3[0]
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.collections.MutableList<*> & T!!")!>prop_3<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.collections.MutableList<*> & T!!")!>prop_3<!>[0]
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.String & T!!")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.String & T!!")!>this<!>.length
        length
    }
}
