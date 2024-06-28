// FIR_IDENTICAL
// FILE: p/Foo.java
package p;

public class Foo {
   protected static class Nested {} 
}

// FILE: foo.kt
package a

import p.Foo
import p.Foo.Nested

class Bar : Foo() {
    protected fun foo(): Nested? = null
}

private fun foo(): <!INVISIBLE_REFERENCE!>Nested<!>? = null
private fun bar(): p.Foo.<!INVISIBLE_REFERENCE!>Nested<!>? = null
