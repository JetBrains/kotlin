// UNRESOLVED_REFERENCE

// FILE: TestClass.hidden.java
package foo;

public class TestClass {
    public TestClass() {}
}

// FILE: main.kt
package foo

fun main() {
    val x = <caret>TestClass()
}