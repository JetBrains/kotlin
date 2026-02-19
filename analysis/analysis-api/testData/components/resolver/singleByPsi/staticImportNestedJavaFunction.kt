// FILE: Dependency.java
public class Dependency {
    public static class Nested {
        public static void foo() {

        }

        public static void foo(int i) {

        }
    }
}

// FILE: main.kt
package another

import Dependency.Nested.foo

fun usage() {
    <expr>foo()</expr>
}
