// !CHECK_TYPE

//KT-2631 Check multiple assignment
package a

class MyClass {
    fun component1() = 1
    fun component2() = "a"
}

class MyClass2 {}

fun MyClass2.component1() = 1.2

fun test(mc1: MyClass, mc2: MyClass2) {
    val (a, b) = mc1
    checkSubtype<Int>(a)
    checkSubtype<String>(b)

    val (c) = mc2
    checkSubtype<Double>(c)

    //check no error types
    checkSubtype<Boolean>(<!TYPE_MISMATCH!>a<!>)
    checkSubtype<Boolean>(<!TYPE_MISMATCH!>b<!>)
    checkSubtype<Boolean>(<!TYPE_MISMATCH!>c<!>)
}