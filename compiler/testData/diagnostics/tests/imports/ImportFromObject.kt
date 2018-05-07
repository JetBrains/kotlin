// FILE: a.kt
package a

object O {
    class A
    object B

    fun bar() {}
}


object S {

    val prop: String = ""

    fun o(<!UNUSED_PARAMETER!>s<!>: String) = Unit
    fun o(<!UNUSED_PARAMETER!>i<!>: Int) = Unit

    fun Int.ext() = Unit
    var String.ext: Int
        get() = 3
        set(<!UNUSED_PARAMETER!>i<!>) {
        }

    fun A(<!UNUSED_PARAMETER!>c<!>: Int) = A()

    class A()

    fun <T> genericFun(t: T, <!UNUSED_PARAMETER!>t2<!>: T): T = t
}

open class Base {
    fun f() {
    }

    fun <T> g(<!UNUSED_PARAMETER!>t<!>: T) {
    }

    val p = 1
    val Int.ext: Int
        get() = 4
}

interface BaseI<T> {
    fun fromI(): Int = 3

    fun genericFromI(t: T) = t
}

object K: Base(), BaseI<Int> {
    val own: String = ""
}

// FILE: b.kt
package b

import a.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>O<!>.*

fun testErroneusAllUnderImportFromObject() {
    <!UNRESOLVED_REFERENCE!>A<!>()
    <!UNRESOLVED_REFERENCE!>B<!>
    <!UNRESOLVED_REFERENCE!>bar<!>()
}

// FILE: c.kt
package c

import a.S.prop
import a.S.o
import a.S.ext
import a.S.A
import a.S.genericFun
import a.S.ext as extRenamed

fun testImportFromObjectByName() {
    prop
    o("a")
    o(3)
    3.ext()
    "".ext = 3
    val <!UNUSED_VARIABLE!>c<!>: Int = "".ext

    3.extRenamed()
    "".extRenamed = 3
    val <!UNUSED_VARIABLE!>c2<!>: Int = "".extRenamed

    A()
    A(3)

    val <!UNUSED_VARIABLE!>a<!>: Int = genericFun(3, 3)
    val <!UNUSED_VARIABLE!>s<!>: String = genericFun("A", "b")
    val <!UNUSED_VARIABLE!>b<!>: Boolean = genericFun(true, false)
}

fun <T> t(t: T): T {
    return genericFun(t, t)
}

// FILE: d.kt
package d

import a.S.prop as renamed

fun testFunImportedFromObjectHasNoDispatchReceiver(l: a.S) {
    l.<!UNRESOLVED_REFERENCE!>renamed<!>
    l.prop
    renamed
}

// FILE: e.kt

package e

import a.K.f
import a.K.g
import a.K.p
import a.K.own
import a.K.fromI
import a.K.genericFromI
import a.K.ext

fun testMembersFromSupertypes() {
    f()
    g("")
    p
    fromI()

    genericFromI(3)
    genericFromI(<!TYPE_MISMATCH!>"a"<!>)

    own
}