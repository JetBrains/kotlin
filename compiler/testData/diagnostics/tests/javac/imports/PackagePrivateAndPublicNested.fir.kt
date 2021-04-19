// FILE: p/Foo.java
package p;

class Foo {
    public static class Nested {}
}

// FILE: foo.kt
package a

import p.Foo
import p.Foo.Nested

class Bar : <!EXPOSED_SUPER_CLASS, INVISIBLE_REFERENCE!>Foo<!>() {
    protected fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(): Nested? = null
}

private fun foo(): Nested? = null
private fun bar(): p.Foo.Nested? = null
