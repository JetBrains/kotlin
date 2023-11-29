// ISSUE: KT-40510

// FILE: foo/A.java
package foo;

public abstract class A {
    // package-private
    abstract void foo();
}

// FILE: main.kt
import foo.A

class <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>DelegatedA<!>(val a: A) : <!DELEGATION_NOT_TO_INTERFACE, SUPERTYPE_NOT_INITIALIZED!>A<!> by a
