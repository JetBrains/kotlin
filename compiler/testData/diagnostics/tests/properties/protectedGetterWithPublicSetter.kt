// FILE: j/Super.java
package j

public abstract class Super {
    protected abstract String getName();
    public abstract void setName(String s);
}

// FILE: k/test.kt

package k
import j.Super

abstract class Sub: Super() {
    fun test(s: Super) {
        s.<!INVISIBLE_MEMBER!>name<!>
        s.<!INVISIBLE_MEMBER!>getName<!>()
        s.name = ""
        s.name = s.<!INVISIBLE_MEMBER!>name<!>
        s.setName("")
    }
}

fun test(s: Super) {
    s.<!INVISIBLE_MEMBER!>name<!>
    s.<!INVISIBLE_MEMBER!>getName<!>()
    s.<!INVISIBLE_MEMBER!>name<!> = ""
    s.<!INVISIBLE_MEMBER!>name<!> = s.<!INVISIBLE_MEMBER!>name<!>
    s.setName("")
}
