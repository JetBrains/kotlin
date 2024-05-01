// ISSUE: KT-57600

// FILE: I.java
public interface I<T> {
    Box<T> foo(Box<T> box);
}

// FILE: Base.java
public class Base implements I<String> {
    @Override
    public Box<String> foo(Box box) { // raw type here
        return box;
    }
}

// FILE: Derived.kt
class Box<T>

class Derived : Base() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(box: Box<String>): Box<String> {
        return box
    }
}
