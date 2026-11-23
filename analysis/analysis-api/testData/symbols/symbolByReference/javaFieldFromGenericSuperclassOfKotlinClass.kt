// ISSUE: KT-92046
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// FILE: main.kt
class Derived : Base<String>()

fun some(derived: Derived) {
    derived.va<caret>lue
}

// FILE: Base.java
public class Base<T> {
    public T value;
}
