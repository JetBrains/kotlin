// FILE: Derived.kt

class Derived : Some()

// FILE: Some.java

public class Some implements Strange {
    public Object foo() {
        return "";
    }
}

// FILE: Strange.kt

interface Strange<out T> {
    fun foo(): T
}