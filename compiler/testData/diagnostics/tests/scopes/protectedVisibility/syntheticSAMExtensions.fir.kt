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
        a.<!INVISIBLE_REFERENCE!>foo<!> { }

        b.foo { }

        if (a is B) {
            a.foo {}
        }

        if (d.x is B) {
            d.x.<!INVISIBLE_REFERENCE!>foo<!> {}
        }
    }
}

fun baz(a: A) {
    a.<!INVISIBLE_REFERENCE!>foo<!> { }
}
