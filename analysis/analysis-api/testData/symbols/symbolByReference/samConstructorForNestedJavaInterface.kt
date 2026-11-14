// FILE: main.kt
package test

fun usage() {
    JavaOuter.<caret>Nested { }
}

// FILE: test/JavaOuter.java
package test;

public class JavaOuter {
    public interface Nested {
        void f();
    }
}
