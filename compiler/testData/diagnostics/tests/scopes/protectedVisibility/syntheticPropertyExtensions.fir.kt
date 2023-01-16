// FILE: abc/A.java
package abc;
public class A {
    public int getAbc() {}
    protected int getFoo() { return 1; }

    public String getBar() { return ""; }
    protected void setBar(String x) {  }
}

// FILE: main.kt
import abc.A

class Data(var x: A)

class B : A() {
    fun baz(a: A, b: B, d: Data) {
        foo
        bar = bar + ""

        b.foo
        b.bar = b.bar + ""

        a.<!INVISIBLE_REFERENCE!>foo<!>
        a.<!INVISIBLE_SETTER!>bar<!> = a.bar + ""

        if (a is B) {
            a.foo
            a.bar = a.bar + ""
        }

        if (d.x is B) {
            d.x.abc // Ok
            d.x.<!INVISIBLE_REFERENCE!>foo<!>
            d.x.<!INVISIBLE_SETTER!>bar<!> = d.x.bar + ""
        }
    }
}

fun baz(a: A) {
    a.<!INVISIBLE_REFERENCE!>foo<!>
    a.<!INVISIBLE_SETTER!>bar<!> = a.bar + ""
}
