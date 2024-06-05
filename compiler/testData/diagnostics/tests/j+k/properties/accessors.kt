// FIR_IDENTICAL
// FILE: A.java

public class A {
    public int getFoo() { return 0; }
}

// FILE: main.kt

class B : A() {
    <!NOTHING_TO_OVERRIDE_ACCESSORS!>override<!> val foo: Int = 2
}
