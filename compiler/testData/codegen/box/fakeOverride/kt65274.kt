// TARGET_BACKEND: JVM

// FILE: a/A.java
package a;

public class A {
    static final String X = "Fail";
}

// FILE: B.java
public class B extends a.A {
    private final String X = "OK";

    public String get() { return X; }
}

// FILE: box.kt
class C : B()

fun box(): String = C().get()