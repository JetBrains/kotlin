// FILE: BaseClass.java
public class BaseClass {
    public static void foo() {}
    public static void foo(int i) {}
}

// FILE: main.kt
package another

import BaseClass.foo

fun usage() {
    <expr>foo()</expr>
}