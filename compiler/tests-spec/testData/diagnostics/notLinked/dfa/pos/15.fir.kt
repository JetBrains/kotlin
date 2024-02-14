// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 15
 * DESCRIPTION: Raw data flow analysis test
 * NOTE: performance test
 * HELPERS: classes, interfaces, functions, properties
 */

// TESTCASE NUMBER: 1
open class Case1_1 : InterfaceWithTypeParameter1<Case1_1>
open class Case1_2 : InterfaceWithTypeParameter1<Case1_2>

fun case_1() {
    val a = select(Case1_1(), Case1_2(), null)

    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter1<*>")!>a<!>
        val b = <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter1<*>")!>a<!>.ip1test1()
        if (b != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>b<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 2
class Case2_1 : Interface3, InterfaceWithTypeParameter1<Case2_1>
class Case2_2 : Interface3, InterfaceWithTypeParameter1<Case2_2>

fun case_2() {
    val x = select(Case2_1(), Case2_2(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithTypeParameter1<*>? & Interface3 & InterfaceWithTypeParameter1<*>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithTypeParameter1<*>? & Interface3 & InterfaceWithTypeParameter1<*>")!>x<!>.ip1test1()
    }
}

// TESTCASE NUMBER: 3
class Case3_1 : Interface3, InterfaceWithTypeParameter1<Case3_1>, InterfaceWithTypeParameter2<Case3_1>
class Case3_2 : Interface3, InterfaceWithTypeParameter1<Case3_2>, InterfaceWithTypeParameter2<Case3_2>

fun case_3() {
    val x = select(Case3_1(), Case3_2(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter2<*>? & Interface3 & InterfaceWithTypeParameter1<*> & InterfaceWithTypeParameter2<*>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter2<*>? & Interface3 & InterfaceWithTypeParameter1<*> & InterfaceWithTypeParameter2<*>")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter2<*>? & Interface3 & InterfaceWithTypeParameter1<*> & InterfaceWithTypeParameter2<*>")!>x<!>.ip1test2()
    }
}

// TESTCASE NUMBER: 4
class Case4_1 : InterfaceWithTypeParameter1<Case4_2>, InterfaceWithTypeParameter2<Case4_1>
class Case4_2 : InterfaceWithTypeParameter1<Case4_1>, InterfaceWithTypeParameter2<Case4_2>

fun case_4() {
    val x = select(Case4_1(), Case4_2(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter2<*>? & InterfaceWithTypeParameter1<*> & InterfaceWithTypeParameter2<*>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter2<*>? & InterfaceWithTypeParameter1<*> & InterfaceWithTypeParameter2<*>")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<*>? & InterfaceWithTypeParameter2<*>? & InterfaceWithTypeParameter1<*> & InterfaceWithTypeParameter2<*>")!>x<!>.ip1test2()
    }
}

// TESTCASE NUMBER: 5
class Case5_1 : InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>>, InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>
class Case5_2 : InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>, InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>>

fun case_5() {
    val x = select(Case5_1(), Case5_2(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>>? & InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>? & InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>> & InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>>? & InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>? & InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>> & InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>>? & InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>? & InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<Case5_2>> & InterfaceWithTypeParameter2<InterfaceWithTypeParameter1<Case5_1>>")!>x<!>.ip1test2()
    }
}

// TESTCASE NUMBER: 6
class Case6_1<T> : InterfaceWithTypeParameter1<InterfaceWithTypeParameter2<T>>, InterfaceWithTypeParameter2<InterfaceWithTypeParameter2<Case6_1<T>>>
class Case6_2<T> : InterfaceWithTypeParameter2<InterfaceWithTypeParameter2<Case6_2<T>>>, InterfaceWithTypeParameter1<InterfaceWithTypeParameter2<T>>

fun case_6() {
    val x = select(Case6_1<Int>(), Case6_2<Float>(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<out kotlin.Number & kotlin.Comparable<*>>>? & InterfaceWithTypeParameter2<out InterfaceWithTypeParameter2<out InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<*>> & InterfaceWithTypeParameter2<*>>>? & InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<out kotlin.Number & kotlin.Comparable<*>>> & InterfaceWithTypeParameter2<out InterfaceWithTypeParameter2<out InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<*>> & InterfaceWithTypeParameter2<*>>>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<out kotlin.Number & kotlin.Comparable<*>>>? & InterfaceWithTypeParameter2<out InterfaceWithTypeParameter2<out InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<*>> & InterfaceWithTypeParameter2<*>>>? & InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<out kotlin.Number & kotlin.Comparable<*>>> & InterfaceWithTypeParameter2<out InterfaceWithTypeParameter2<out InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<*>> & InterfaceWithTypeParameter2<*>>>")!>x<!>.ip1test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<out kotlin.Number & kotlin.Comparable<*>>>? & InterfaceWithTypeParameter2<out InterfaceWithTypeParameter2<out InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<*>> & InterfaceWithTypeParameter2<*>>>? & InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<out kotlin.Number & kotlin.Comparable<*>>> & InterfaceWithTypeParameter2<out InterfaceWithTypeParameter2<out InterfaceWithTypeParameter1<out InterfaceWithTypeParameter2<*>> & InterfaceWithTypeParameter2<*>>>")!>x<!>.ip1test2()
    }
}

// TESTCASE NUMBER: 7
open class Case7_1<T, K> : InterfaceWithTwoTypeParameters<Inv<T>, Inv<K>>
open class Case7_2<T, K> : InterfaceWithTwoTypeParameters<Inv<K>, Inv<T>>

fun case_7() {
    val x = select(Case7_1<Int, Float>(), Case7_2<Char, String>(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<out Inv<out kotlin.Comparable<*> & java.io.Serializable>, out Inv<out kotlin.Comparable<*> & java.io.Serializable>>? & InterfaceWithTwoTypeParameters<out Inv<out kotlin.Comparable<*> & java.io.Serializable>, out Inv<out kotlin.Comparable<*> & java.io.Serializable>>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<out Inv<out kotlin.Comparable<*> & java.io.Serializable>, out Inv<out kotlin.Comparable<*> & java.io.Serializable>>? & InterfaceWithTwoTypeParameters<out Inv<out kotlin.Comparable<*> & java.io.Serializable>, out Inv<out kotlin.Comparable<*> & java.io.Serializable>>")!>x<!>.ip2test()
    }
}

// TESTCASE NUMBER: 8
open class Case8_1<T, K> : ClassWithTwoTypeParameters<ClassWithTwoTypeParameters<T, K>, K>()
open class Case8_2<T, K> : ClassWithTwoTypeParameters<ClassWithTwoTypeParameters<K, T>, T>()

fun case_8() {
    val x = select(Case8_1<Int, Float>(), Case8_2<Char, String>(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<out ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>, out kotlin.Comparable<*> & java.io.Serializable>? & ClassWithTwoTypeParameters<out ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>, out kotlin.Comparable<*> & java.io.Serializable>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<out ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>, out kotlin.Comparable<*> & java.io.Serializable>? & ClassWithTwoTypeParameters<out ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>, out kotlin.Comparable<*> & java.io.Serializable>")!>x<!>.test1()
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<out ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>, out kotlin.Comparable<*> & java.io.Serializable>? & ClassWithTwoTypeParameters<out ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>, out kotlin.Comparable<*> & java.io.Serializable>")!>x<!>.test2()
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>? & ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>? & ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>")!>y<!>.test1()
            val z = <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>? & ClassWithTwoTypeParameters<out kotlin.Comparable<*> & java.io.Serializable, out kotlin.Comparable<*> & java.io.Serializable>")!>y<!>.test2()
            if (z != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & java.io.Serializable? & kotlin.Comparable<*> & java.io.Serializable")!>z<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 9
open class Case9_1<T, K> : ClassWithTwoTypeParameters<Case9_1<T, K>, Case9_2<K, T>>()
open class Case9_2<T, K> : ClassWithTwoTypeParameters<Case9_2<K, T>, Case9_1<T, K>>()

fun case_9() {
    val x = select(Case9_1<Int, String>(), Case9_2<Float, Char>(), null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<*, *>? & ClassWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithTwoTypeParameters<*, *>? & ClassWithTwoTypeParameters<*, *>")!>x<!>.test1()
        val y = x.test2()
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>y<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 10
open class Case10_1 : Interface3, InterfaceWithOutParameter<Case10_1>
open class Case10_2 : Interface3, InterfaceWithOutParameter<Case10_2>

fun case_10() = run {
    val x = select(object : Case10_1() {}, object : Case10_2() {}, null)

    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3? & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>? & Interface3 & InterfaceWithOutParameter<Interface3 & InterfaceWithOutParameter<*>>")!>x<!>.funNullableAny()
    }
}
