// FILE: Base.java

public class Base {
    public int value = 0;
}

// FILE: Derived.kt

class Derived : Base() {
    fun getValue() = value

    fun foo() = value
}
