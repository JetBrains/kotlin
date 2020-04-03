// !WITH_NEW_INFERENCE
// FILE:a.kt
package a

import b.B        //class
import b.foo      //function
import b.ext      //extension function
import b.value    //property
import b.C.Companion.bar    //function from companion object
import b.C.Companion.cValue //property from companion object
import b.<!UNRESOLVED_REFERENCE!>constant<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>fff<!>     //function from val
import b.<!UNRESOLVED_REFERENCE!>constant<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>dValue<!>  //property from val
import b.constant
import b.E.Companion.f      //val from companion object
import <!UNRESOLVED_REFERENCE!>smth<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>illegal<!>
import b.C.<!UNRESOLVED_REFERENCE!>smth<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>illegal<!>
import b.<!UNRESOLVED_REFERENCE!>bar<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>smth<!>
import b.<!UNRESOLVED_REFERENCE!>bar<!>.*
import b.<!UNRESOLVED_REFERENCE!>unr<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>unr<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>unr<!>
import <!UNRESOLVED_REFERENCE!>unr<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>unr<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>unr<!>

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

import c.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>C<!>.*

object C {
    fun f() {
    }
    val i = 348
}

fun foo() {
    if (<!UNRESOLVED_REFERENCE!>i<!> <!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>==<!> 3) <!UNRESOLVED_REFERENCE!>f<!>()
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