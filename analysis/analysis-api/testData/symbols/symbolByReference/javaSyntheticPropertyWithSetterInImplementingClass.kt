// ISSUE: KT-91799
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// FILE: main.kt
fun some(derived: Derived) {
    derived.f<caret>oo
}

// FILE: Base.java
public interface Base {
    String getFoo();
}

// FILE: Derived.java
public abstract class Derived implements Base {
    public void setFoo(String value) {}
}
