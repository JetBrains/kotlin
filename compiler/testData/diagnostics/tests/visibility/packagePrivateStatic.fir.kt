// ISSUE: KT-54125
// FILE: foo/Base.java
package foo;

class Base {
    protected static void foo() {}
}

// FILE: foo/Derived.java
package foo;

public class Derived extends Base {}

// FILE: main.kt
package bar

import foo.Derived

class Impl : Derived() {
    fun test() {
        <!INVISIBLE_REFERENCE!>foo<!>()
    }
}
