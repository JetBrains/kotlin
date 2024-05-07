// FILE: BaseClass.java
public class BaseClass {
    public static void foo() {}
}

// FILE: main.kt
package another

import BaseClass.foo

fun usage() {
    <expr>foo()</expr>
}