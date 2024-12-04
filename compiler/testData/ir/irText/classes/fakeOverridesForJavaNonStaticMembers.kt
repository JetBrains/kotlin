// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FILE: fakeOverridesForJavaNonStaticMembers.kt
package a

class Test : Base()

// FILE: fakeOverridesForJavaNonStaticMembers2.kt

import a.Base

class Test2 : Base()

// FILE: a/Base.java
package a

public class Base {
    public void publicMethod() {}
    protected void protectedMethod() {}
    void packagePrivateMethod() {}
    private void privateMethod() {}
}
