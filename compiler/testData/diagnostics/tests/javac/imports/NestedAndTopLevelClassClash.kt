// FIR_IDENTICAL
// SKIP_JAVAC
// FILE: a/B.java
package a;

public class B {}

// FILE: a/D.java
package a;

public class D {
    public static class B {}
}

// FILE: b/A1.java
package b;

import a.B;
import a.D.B;

public class A1 {
    public B getB() { return null; }
}

// FILE: b/A2.java
package b;

import a.*;
import a.D.*;

public class A2 {
    public B getB() { return null; }
}

// FILE: a.kt
package b

fun test() = A1().getB()
fun test2() = A2().<!MISSING_DEPENDENCY_CLASS!>getB<!>()
