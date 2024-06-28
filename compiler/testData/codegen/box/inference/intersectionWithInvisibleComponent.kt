// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62544

// FILE: Base.java
public interface Base {
    String get();
}

// FILE: Impl.java
class Impl {}

// FILE: O.java
public class O extends Impl implements Base {
    @Override
    public String get() { return "O"; }
}

// FILE: K.java
public class K extends Impl implements Base {
    @Override
    public String get() { return "K"; }
}

// FILE: box.kt
package test

import O
import K

fun <A, B : A, C : A> foo(
    b: B,
    c: C,
    f: (A) -> String
): String {
    return f(b) + f(c)
}

fun box(): String {
    return foo(O(), K()) { it.get() }
}
