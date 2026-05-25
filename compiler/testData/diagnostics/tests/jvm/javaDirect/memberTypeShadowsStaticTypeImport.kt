
// FILE: a/C.java
package a;

public class C {
    public static class Inner<F> {
        public int fromImported() { return 1; }
    }
}

// FILE: b/MyJavaClass.java
package b;

import static a.C.Inner;

public class MyJavaClass {
    public static class Inner<F> {
        public int fromMember() { return 7; }
    }

    public static Inner<Object> make() { return new Inner<Object>(); }
}

// FILE: main.kt
package b

fun test() = MyJavaClass.make().fromMember()

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType */
