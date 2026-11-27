// ISSUE: KT-92046
// FILE: main.kt
class Derived : Base<String>()

fun some(derived: Derived) {
    derived.va<caret>lue
}

// FILE: Base.java
public class Base<T> {
    public T value;
}
