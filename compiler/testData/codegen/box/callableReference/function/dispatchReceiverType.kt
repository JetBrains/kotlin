// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: test/X.java
package test;

public class X extends PX {
    public X(String x) { super(x); }
}

// FILE: test/PX.java
package test;

class PX {
    private final String x;

    PX(String x) { this.x = x; }

    public String foo() { return x; }
}

// MODULE: main(lib)
// FILE: box.kt
import test.X

fun <T, R> T.myLet(block: (T) -> R): R = block(this)

fun box() = X("O").let(X::foo) + X("K").myLet(X::foo)
