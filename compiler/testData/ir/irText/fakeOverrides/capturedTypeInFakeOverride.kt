// FIR_IDENTICAL

// FILE: Jaba.java
public class Jaba {
    public int foo = 42;
}

// FILE: main.kt
class Foo : Jaba() {
    // var foo = 42
}
