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
        a.bar = t.bar
        a.foo = t.foo

        if (d.x is B) {
            d.x.bar = d.x.bar + ""
            d.x.foo = d.x.foo + ""
        }
    }
}
