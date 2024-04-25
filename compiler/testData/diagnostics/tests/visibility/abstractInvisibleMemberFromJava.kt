// FIR_IDENTICAL
// LANGUAGE: +ProhibitInvisibleAbstractMethodsInSuperclasses
// FILE: base/Base.java
package base;

public abstract class Base {
    public void foo() {
        packagePrivateFoo();
    }

    /* package-private */ abstract void packagePrivateFoo();
}

// FILE: Impl.kt
package impl
import base.*

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Impl<!> : Base()

fun foo() {
    Impl().foo()
}
