
// FILE: foo/Base.java
package foo;

public class Base {
    public void foo() {}
}

// FILE: foo/Derived.java
package foo;

public class Derived extends Base {}

// FILE: main.kt
import foo.Derived

fun test(x: Derived) {
    x.foo()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType */
