package exceptions

import java.util.ArrayList

fun throwException() {
    // EXPRESSION: fail()
    // RESULT: Method threw 'java.lang.UnsupportedOperationException' exception.
    //Breakpoint!
    val a = 1

    // EXPRESSION: fail()
    // RESULT: Method threw 'java.lang.UnsupportedOperationException' exception.
    //Breakpoint!
    val b = 1
}

fun fail() {
    throw UnsupportedOperationException()
}

fun classCast() {
    val o = Base()
    // EXPRESSION: o as Derived
    // RESULT: java.lang.ClassCastException: exceptions.Base cannot be cast to exceptions.Derived: Ljava/lang/ClassCastException;
    //Breakpoint!
    val a = 1

    // EXPRESSION: o as Derived
    // RESULT: java.lang.ClassCastException: exceptions.Base cannot be cast to exceptions.Derived: Ljava/lang/ClassCastException;
    //Breakpoint!
    val b = 1
}


fun genericClassCast() {
    if (true) {
        val c = ArrayList<Int>()
        c.add(1)
        // EXPRESSION: c.get(0)
        // RESULT: 1: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val c = ArrayList<String>()
        c.add("a")
        // EXPRESSION: c.get(0)
        // RESULT: java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Number: Ljava/lang/ClassCastException;
        //Breakpoint!
        val b = 1
    }
}

open class Base {
    private fun test(): Int = 1
}

class Derived: Base()

fun main(args: Array<String>) {
    throwException()
    classCast()
    genericClassCast()
}