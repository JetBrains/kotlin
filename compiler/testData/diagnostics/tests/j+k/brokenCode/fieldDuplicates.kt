// FILE: A.java

public class A {
    public String foo;
    public String foo = "";
}

// FILE: main.kt

fun foo() {
    // no exception is thrown (see KT-3898)
    A().foo
}
