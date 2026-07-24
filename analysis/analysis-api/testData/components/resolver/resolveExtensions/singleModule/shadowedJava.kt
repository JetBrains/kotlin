// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated
// RESOLVE_EXTENSION_SHADOWED: \.hidden\.[a-z]+$

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