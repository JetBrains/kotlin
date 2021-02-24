// ISSUE: KT-40510

// FILE: foo/A.java
package foo;

public abstract class A {
    // package-private
    abstract void foo();
}

// FILE: main.kt
import foo.A

class DelegatedA(val a: A) : A by a
