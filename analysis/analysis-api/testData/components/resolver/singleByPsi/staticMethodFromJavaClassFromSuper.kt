// FILE: BaseClass.java
public class BaseClass {
    public static void foo() {}
    public static void foo(int i) {}
}

// FILE: Child.java
public class Child extends BaseClass {

}

// FILE: main.kt
package another

import Child.foo

fun usage() {
    <expr>foo()</expr>
}