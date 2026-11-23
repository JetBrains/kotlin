// ISSUE: KT-92045
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// Neither class of the accessors has both of them in its scope, so the pointer restores the property without the setter
// FILE: main.kt
fun some(both: Both) {
    both.f<caret>oo
}

// FILE: Getter.java
public interface Getter {
    String getFoo();
}

// FILE: Setter.java
public interface Setter {
    void setFoo(String value);
}

// FILE: Both.java
public interface Both extends Getter, Setter {
}
