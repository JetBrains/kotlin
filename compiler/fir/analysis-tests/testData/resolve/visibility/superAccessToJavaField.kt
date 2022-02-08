// ISSUE: KT-49654
// FULL_JDK

// FILE: Base.java
public interface Base {
    String getParent();
}

// FILE: Derived.java
public class Derived implements Base {
    protected String parent = "";

    public String getParent() {
        return parent;
    }
}

// FILE: main.kt

interface MyBase : Base

abstract class Implementation : Derived(), MyBase {
    val parentNode: String? get() = super.parent
}
