
// FILE: a/C.java
package a;

public class C {
    public static int foo() { return 0; }
}

// FILE: b/foo.java
package b;

public class foo {
    public int value = 42;
}

// FILE: b/T.java
package b;

import static a.C.foo;

public class T {
    public foo getFoo() { return new foo(); }
}

// FILE: main.kt
package b

fun test() = T().getFoo().value

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaProperty, javaType */
