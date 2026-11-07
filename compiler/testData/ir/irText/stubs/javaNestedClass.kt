// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: J
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib reads Java declarations from compiled classes, not from sources.
// FILE: J.java

public class J {
    public static class JJ {
        public void foo() {}
        public static void bar() {}
    }
}


// FILE: javaNestedClass.kt
fun test(jj: J.JJ) = jj.foo()
