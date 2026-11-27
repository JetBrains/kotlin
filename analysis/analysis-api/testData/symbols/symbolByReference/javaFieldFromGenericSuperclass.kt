// ISSUE: KT-92046
// FILE: main.kt
fun some(derived: Derived) {
    derived.va<caret>lue
}

// FILE: Base.java
public class Base<T> {
    public T value;
}

// FILE: Derived.java
public class Derived extends Base<String> {
}
