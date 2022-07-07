// WITH_STDLIB

// FILE: j/J.java
package j;

public class J {
    public int getX() { return 1; }
    protected void setX(int value) { throw new RuntimeException(); }
}

// FILE: main.kt
import j.*

class C : J() {
    fun foo() {
        <!INVISIBLE_SETTER!>J()<!UNNECESSARY_SAFE_CALL!>?.<!>x<!> = 1
    }
}

fun box(): String {
    C().foo()
    return "OK"
}