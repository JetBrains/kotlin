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
    <!VIRTUAL_MEMBER_HIDDEN!>// We keep this test to make sure ACCIDENTAL_OVERRIDE is not reported
    fun <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>foo<!>(f: () -> Unit)<!> {}
}