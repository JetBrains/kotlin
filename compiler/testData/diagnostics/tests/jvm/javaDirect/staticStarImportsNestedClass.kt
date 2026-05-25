
// FILE: b/C.java
package b;

public class C {
    public static class X {
        public int fromBC() { return 2; }
    }
}

// FILE: c/T.java
package c;

import static b.C.*;

public class T {
    public X getX() { return new X(); }
}

// FILE: main.kt
package c

fun test() = T().getX().fromBC()

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType */
