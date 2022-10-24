// FIR_IDENTICAL
// ISSUE: KT-53441
// FILE: foo/Base.java
package foo;

class Base {
    protected static void foo() {}
    protected void bar() {}
}

// FILE: foo/Derived.java
package foo;

public class Derived extends Base {}

// FILE: main.kt
package bar

import foo.Derived

class Impl : Derived() {
    fun test() {
        foo()
        bar()
    }
}
