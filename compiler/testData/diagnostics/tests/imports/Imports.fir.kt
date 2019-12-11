// !WITH_NEW_INFERENCE
// FILE:a.kt
package a

import b.B        //class
import b.foo      //function
import b.ext      //extension function
import b.value    //property
import b.C.Companion.bar    //function from companion object
import b.C.Companion.cValue //property from companion object
import b.constant.fff     //function from val
import b.constant.dValue  //property from val
import b.constant
import b.E.Companion.f      //val from companion object
import smth.illegal
import b.C.smth.illegal
import b.bar.smth
import b.bar.*
import b.unr.unr.unr
import unr.unr.unr

fun test(arg: B) {
    foo(value)
    arg.ext()

    bar()
    foo(cValue)

    <!UNRESOLVED_REFERENCE!>fff<!>(<!UNRESOLVED_REFERENCE!>dValue<!>)

    constant.fff(constant.dValue)

    f.f()
}

// FILE:b.kt
package b

class B() {}

fun foo(i: Int) = i

fun B.ext() {}

val value = 0

class C() {
    companion object {
        fun bar() {}
        val cValue = 1
    }
}

class D() {
    fun fff(s: String) = s
    val dValue = "w"
}

val constant = D()

class E() {
    companion object {
        val f = F()
    }
}

class F() {
    fun f() {}
}

fun bar() {}

//FILE:c.kt
package c

import c.C.*

object C {
    fun f() {
    }
    val i = 348
}

fun foo() {
    if (i == 3) f()
}

//FILE:d.kt
package d

import d.A.Companion.B
import d.A.Companion.C

val b : B = B()
val c : B = C

class A() {
    companion object {
        open class B() {}
        object C : B() {}
    }
}