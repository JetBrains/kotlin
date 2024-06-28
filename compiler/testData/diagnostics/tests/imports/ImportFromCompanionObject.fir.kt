// FILE: a.kt
package a

class C1 {
    companion object O {
        class A
        object B

        fun bar() {}
    }
}


class C2 {
    companion object S {

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

class C3 {
    companion object K: Base(), BaseI<Int> {
        val own: String = ""
    }
}

// FILE: b.kt
package b

import a.C1.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>O<!>.*

fun testErroneusAllUnderImportFromObject() {
    A()
    B
    bar()
}

// FILE: c.kt
package c

import a.C2.S.prop
import a.C2.S.o
import a.C2.S.ext
import a.C2.S.A
import a.C2.S.genericFun
import a.C2.S.ext as extRenamed

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

import a.C2.S.prop as renamed

fun testFunImportedFromObjectHasNoDispatchReceiver(l: a.C2.S) {
    l.<!UNRESOLVED_REFERENCE!>renamed<!>
    l.prop
    renamed
}

// FILE: e.kt

package e

import a.C3.K.f
import a.C3.K.g
import a.C3.K.p
import a.C3.K.own
import a.C3.K.fromI
import a.C3.K.genericFromI
import a.C3.K.ext

fun testMembersFromSupertypes() {
    f()
    g("")
    p
    fromI()

    genericFromI(3)
    genericFromI(<!ARGUMENT_TYPE_MISMATCH!>"a"<!>)

    own
}
