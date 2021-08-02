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

        a.<!INVISIBLE_MEMBER!>foo<!>
        // TODO: should be INVISIBLE_SETTER
        a.<!INVISIBLE_SETTER!>bar<!> = a.bar + ""

        if (a is B) {
            <!DEBUG_INFO_SMARTCAST!>a<!>.foo
            <!DEBUG_INFO_SMARTCAST!>a<!>.bar = <!DEBUG_INFO_SMARTCAST!>a<!>.bar + ""
        }

        if (d.x is B) {
            d.x.abc // Ok
            d.x.<!INVISIBLE_MEMBER!>foo<!>
            // TODO: should be INVISIBLE_SETTER
            d.x.<!INVISIBLE_SETTER!>bar<!> = d.x.bar + ""
        }
    }
}

fun baz(a: A) {
    a.<!INVISIBLE_MEMBER!>foo<!>
    <!INVISIBLE_SETTER!>a.<!INVISIBLE_SETTER!>bar<!><!> = a.bar + ""
}
