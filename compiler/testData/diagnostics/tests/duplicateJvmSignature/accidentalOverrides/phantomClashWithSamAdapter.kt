// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: p/Sam.java

package p;

public interface Sam {
    void sam();
}

// FILE: p/Foo.java

package p;

public class Foo {
    public void foo(Sam sam);
}

// FILE: k.kt

import p.*

// to have enough fake overrides
open class K0 : Foo()

class K : K0() {
    fun foo(f: () -> Unit) {}
}