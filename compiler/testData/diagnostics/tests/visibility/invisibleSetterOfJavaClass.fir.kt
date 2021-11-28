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
        <!INVISIBLE_SETTER!>a.bar<!> = t.bar
        <!INVISIBLE_SETTER!>a.foo<!> = t.foo

        if (d.x is B) {
            <!INVISIBLE_SETTER!>d.x.bar<!> = d.x.bar + ""
            <!INVISIBLE_SETTER!>d.x.foo<!> = d.x.foo + ""
        }
    }
}
