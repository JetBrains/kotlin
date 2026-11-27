// ISSUE: KT-91799
// FILE: main.kt
fun some() {
    Derived().f<caret>oo
}

// FILE: Base.java
public class Base {
    public String getFoo() { return ""; }
    public void setFoo(String value) {}
}

// FILE: Derived.java
public class Derived extends Base {
    @Override
    public String getFoo() { return ""; }
}
