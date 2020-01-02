// !DUMP_DEPENDENCIES
// FILE: J.java

public class J {
    public static class JJ {
        public void foo() {}
        public static void bar() {}
    }
}


// FILE: javaNestedClass.kt
fun test(jj: J.JJ) = jj.foo()