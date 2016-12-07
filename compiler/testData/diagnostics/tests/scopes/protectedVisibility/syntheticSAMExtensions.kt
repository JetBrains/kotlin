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
        a.<!INVISIBLE_MEMBER!>foo<!> <!TYPE_MISMATCH!>{ }<!>

        b.foo { }

        if (a is B) {
            <!DEBUG_INFO_SMARTCAST!>a<!>.foo {}
        }

        if (d.x is B) {
            <!SMARTCAST_IMPOSSIBLE!>d.x<!>.foo <!TYPE_MISMATCH!>{}<!>
        }
    }
}

fun baz(a: A) {
    a.<!INVISIBLE_MEMBER!>foo<!> <!TYPE_MISMATCH!>{ }<!>
}
