// TARGET_BACKEND: JVM
// FILE: p/A.java
package p;
public interface A {
    public static int foo() { return 56; }
}

// FILE: B.java
import static p.A.foo;

public class B implements p.A {
    public static final int V = foo();
}

// FILE: main.kt

val U = B.V

fun box(): String {
    if (U != 56) return "fail"
    return "OK"
}