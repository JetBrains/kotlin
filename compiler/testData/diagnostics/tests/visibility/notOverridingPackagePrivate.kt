// FIR_IDENTICAL
// FILE: base/Base.java
package base;

public abstract class Base {
    public void foo() {
        packagePrivateFoo();
    }

    /* package-private */ void packagePrivateFoo() {};
}

// FILE: Impl.kt
package impl
import base.*

class Impl : Base() {
    fun packagePrivateFoo() { /*not an override*/ }
}

fun foo() {
    Impl().foo()
    Impl().packagePrivateFoo()
}
