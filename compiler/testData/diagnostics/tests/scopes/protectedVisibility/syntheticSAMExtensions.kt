// !WITH_NEW_INFERENCE
// FILE: abc/A.java
package abc;
public class A {
    protected void foo(Runnable x) {}
}

// FILE: main.kt
import abc.A;

class Data(var x: A)

class B : A() {
    fun baz(a: A, b: B, d: Data) {
        a.<!INVISIBLE_MEMBER!>foo<!> { }

        b.foo { }

        if (a is B) {
            <!OI;DEBUG_INFO_SMARTCAST!>a<!>.<!NI;INVISIBLE_MEMBER!>foo<!> {}
        }

        if (d.x is B) {
            <!OI;SMARTCAST_IMPOSSIBLE!>d.x<!>.<!NI;INVISIBLE_MEMBER!>foo<!> {}
        }
    }
}

fun baz(a: A) {
    a.<!INVISIBLE_MEMBER!>foo<!> { }
}
