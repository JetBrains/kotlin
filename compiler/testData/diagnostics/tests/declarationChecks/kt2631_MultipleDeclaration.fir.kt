// !CHECK_TYPE

//KT-2631 Check multiple assignment
package a

import checkSubtype

class MyClass {
    operator fun component1() = 1
    operator fun component2() = "a"
}

class MyClass2 {}

operator fun MyClass2.component1() = 1.2

fun test(mc1: MyClass, mc2: MyClass2) {
    val (a, b) = mc1
    checkSubtype<Int>(a)
    checkSubtype<String>(b)

    val (c) = mc2
    checkSubtype<Double>(c)

    //check no error types
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Boolean>(a)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Boolean>(b)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Boolean>(c)
}
