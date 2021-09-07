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
        s.<!INVISIBLE_REFERENCE!>name<!>
        s.<!INVISIBLE_REFERENCE!>getName<!>()
        s.name = ""
        s.name = s.<!INVISIBLE_REFERENCE!>name<!>
        s.setName("")
    }
}

fun test(s: Super) {
    s.<!INVISIBLE_REFERENCE!>name<!>
    s.<!INVISIBLE_REFERENCE!>getName<!>()
    s.name = ""
    s.name = s.<!INVISIBLE_REFERENCE!>name<!>
    s.setName("")
}
