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

private fun foo(): Nested? = null
private fun bar(): p.Foo.Nested? = null
