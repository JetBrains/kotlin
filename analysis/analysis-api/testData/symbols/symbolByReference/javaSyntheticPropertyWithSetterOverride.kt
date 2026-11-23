// ISSUE: KT-91799
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
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
    public void setFoo(String value) {}
}
