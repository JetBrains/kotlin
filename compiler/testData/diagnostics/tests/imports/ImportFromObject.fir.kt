// FILE: a.kt
package a

object O {
    class A
    object B

    fun bar() {}
}


object S {

    val prop: String = ""

    fun o(s: String) = Unit
    fun o(i: Int) = Unit

    fun Int.ext() = Unit
    var String.ext: Int
        get() = 3
        set(i) {
        }

    fun A(c: Int) = A()

    class A()

    fun <T> genericFun(t: T, t2: T): T = t
}

open class Base {
    fun f() {
    }

    fun <T> g(t: T) {
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
    A()
    B
    bar()
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
    val c: Int = "".ext

    3.extRenamed()
    "".extRenamed = 3
    val c2: Int = "".extRenamed

    A()
    A(3)

    val a: Int = genericFun(3, 3)
    val s: String = genericFun("A", "b")
    val b: Boolean = genericFun(true, false)
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
    genericFromI(<!ARGUMENT_TYPE_MISMATCH!>"a"<!>)

    own
}