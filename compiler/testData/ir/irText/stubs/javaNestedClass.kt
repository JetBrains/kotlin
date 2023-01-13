// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// DUMP_EXTERNAL_CLASS: J
// FILE: J.java

public class J {
    public static class JJ {
        public void foo() {}
        public static void bar() {}
    }
}


// FILE: javaNestedClass.kt
fun test(jj: J.JJ) = jj.foo()
