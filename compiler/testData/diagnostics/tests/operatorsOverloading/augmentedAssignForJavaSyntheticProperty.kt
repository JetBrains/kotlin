// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-54662

// FILE: Base.java
import java.util.Set;

public class Base {
    public Set<Object> getDependsOn() {
        return null;
    }

    public void setDependsOn(Iterable<?> dependsOn) {}
}

// FILE: main.kt
class Derived : Base() {
    fun test(s: String) {
        this.dependsOn += s
    }
}
