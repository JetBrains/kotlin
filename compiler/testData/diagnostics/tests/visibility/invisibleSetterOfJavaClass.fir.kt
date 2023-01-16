// !LANGUAGE: +ImproveReportingDiagnosticsOnProtectedMembersOfBaseClass

// FILE: abc/Foo.java
package abc;

public class Foo {
    public String getBar() { return ""; }
    protected void setBar(String x) {  }
    public String getFoo() { return ""; }
    private void setFoo(String x) {  }
}

// FILE: main.kt

import abc.Foo

class Data(var x: Foo)

class B : Foo() {
    fun baz(a: Foo, t: Foo, d: Data) {
        a.<!INVISIBLE_SETTER!>bar<!> = t.bar
        a.<!INVISIBLE_SETTER!>foo<!> = t.foo

        if (d.x is B) {
            d.x.<!INVISIBLE_SETTER!>bar<!> = d.x.bar + ""
            d.x.<!INVISIBLE_SETTER!>foo<!> = d.x.foo + ""
        }
    }
}
