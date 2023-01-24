// FILE: j/Super.java
package j

public class Super {
    protected String getName() { return "" };
    public void setName(String s) { }
}

// FILE: k/test.kt

package k
import j.Super

abstract class Sub : Super() {
    fun test(s: Super) {
        s.<!INVISIBLE_REFERENCE!>name<!>
        s.<!INVISIBLE_REFERENCE!>getName<!>()
        s.name = ""
        s.name = s.<!INVISIBLE_REFERENCE!>name<!>
        s.setName("")

        val anon1 = object : Super() {
            fun testAnon() {
                s.<!INVISIBLE_REFERENCE!>name<!>
                s.<!INVISIBLE_REFERENCE!>getName<!>()
                s.name = ""
                s.name = s.<!INVISIBLE_REFERENCE!>name<!>
                s.setName("")
            }
        }

        val anon2 = object {
            fun testAnon() {
                s.<!INVISIBLE_REFERENCE!>name<!>
                s.<!INVISIBLE_REFERENCE!>getName<!>()
                s.name = ""
                s.name = s.<!INVISIBLE_REFERENCE!>name<!>
                s.setName("")
            }
        }
    }

    inner class Nested1 : Super() {
        fun test(s: Super) {
            s.<!INVISIBLE_REFERENCE!>name<!>
            s.<!INVISIBLE_REFERENCE!>getName<!>()
            s.name = ""
            s.name = s.<!INVISIBLE_REFERENCE!>name<!>
            s.setName("")
        }
    }

    class Nested2 {
        fun test(s: Super) {
            s.<!INVISIBLE_REFERENCE!>name<!>
            s.<!INVISIBLE_REFERENCE!>getName<!>()
            s.name = ""
            s.name = s.<!INVISIBLE_REFERENCE!>name<!>
            s.setName("")
        }
    }
}

abstract class NonSub {
    fun test(s: Super) {
        s.<!INVISIBLE_REFERENCE!>name<!>
        s.<!INVISIBLE_REFERENCE!>getName<!>()
        s.<!INVISIBLE_REFERENCE!>name<!> = ""
        s.<!INVISIBLE_REFERENCE!>name<!> = s.<!INVISIBLE_REFERENCE!>name<!>
        s.setName("")

        val anon1 = object : Super() {
            fun testAnon() {
                s.<!INVISIBLE_REFERENCE!>name<!>
                s.<!INVISIBLE_REFERENCE!>getName<!>()
                s.name = ""
                s.name = s.<!INVISIBLE_REFERENCE!>name<!>
                s.setName("")
            }
        }

        val anon2 = object {
            fun testAnon() {
                s.<!INVISIBLE_REFERENCE!>name<!>
                s.<!INVISIBLE_REFERENCE!>getName<!>()
                s.<!INVISIBLE_REFERENCE!>name<!> = ""
                s.<!INVISIBLE_REFERENCE!>name<!> = s.<!INVISIBLE_REFERENCE!>name<!>
                s.setName("")
            }
        }
    }

    inner class Nested1 : Super() {
        fun test(s: Super) {
            s.<!INVISIBLE_REFERENCE!>name<!>
            s.<!INVISIBLE_REFERENCE!>getName<!>()
            s.name = ""
            s.name = s.<!INVISIBLE_REFERENCE!>name<!>
            s.setName("")
        }
    }

    class Nested2 {
        fun test(s: Super) {
            s.<!INVISIBLE_REFERENCE!>name<!>
            s.<!INVISIBLE_REFERENCE!>getName<!>()
            s.<!INVISIBLE_REFERENCE!>name<!> = ""
            s.<!INVISIBLE_REFERENCE!>name<!> = s.<!INVISIBLE_REFERENCE!>name<!>
            s.setName("")
        }
    }
}

fun test(s: Super) {
    s.<!INVISIBLE_REFERENCE!>name<!>
    s.<!INVISIBLE_REFERENCE!>getName<!>()
    s.<!INVISIBLE_REFERENCE!>name<!> = ""
    s.<!INVISIBLE_REFERENCE!>name<!> = s.<!INVISIBLE_REFERENCE!>name<!>
    s.setName("")
}
