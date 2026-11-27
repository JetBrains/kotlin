// ISSUE: KT-91799
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
