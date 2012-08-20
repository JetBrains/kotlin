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
    a : Int
    b : String

    val (c) = mc2
    c : Double

    //check no error types
    <!TYPE_MISMATCH!>a<!> : Boolean
    <!TYPE_MISMATCH!>b<!> : Boolean
    <!TYPE_MISMATCH!>c<!> : Boolean
}