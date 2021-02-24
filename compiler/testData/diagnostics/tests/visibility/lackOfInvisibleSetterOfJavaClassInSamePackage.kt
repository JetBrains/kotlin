// FILE: Foo.java
public class Foo {
    public String getBar() { return ""; }
    protected void setBar(String x) {  }
    public String getFoo() { return ""; }
    private void setFoo(String x) {  }
}

// FILE: main.kt

class Data(var x: Foo)

class B : Foo() {
    fun baz(a: Foo, t: Foo, d: Data) {
        a.bar = t.bar
        <!VAL_REASSIGNMENT!>a.foo<!> = t.foo

        if (d.x is B) {
            d.x.bar = d.x.bar + ""
            d.x.foo = d.x.foo + ""
        }
    }
}
