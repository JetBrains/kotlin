// UNRESOLVED_REFERENCE

// MODULE: extendedModule
// FILE: TestClass.hidden.java
package foo;

public class TestClass {
    public TestClass() {}
}

// MODULE: dependency2

// MODULE: main(extendedModule, dependency2)()()
// FILE: main.kt
package foo

fun main() {
    val x = <caret>TestClass()
}