// FIR_IDENTICAL
// FILE: My.java

public class My {
    static public My create() { return new My(); }
    public void foo() {}
}

// FILE: Test.kt

fun test() {
    val my = My.create()
    if (my == null) {
        my<!UNSAFE_CALL!>.<!>foo()
    }
}
