// !WITH_NEW_INFERENCE
// FILE: My.java

public class My {
    static public My create() { return new My(); }
    public void foo() {}
}

// FILE: Test.kt

fun test() {
    val my = My.create()
    if (my == null) {
        <!DEBUG_INFO_CONSTANT!>my<!><!UNSAFE_CALL!>.<!>foo()
    }
}

