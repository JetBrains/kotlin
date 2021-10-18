// FIR_IDENTICAL
// SKIP_TXT
// FILE: test/JavaBase.java
package test;

abstract /* package-private */ class JavaBase {
    public void foo() {}
}

// FILE: test/JavaBase2.java
package test;
public class JavaBase2 extends JavaBase {}

// FILE: main.kt

import test.*

class KotlinClass : JavaBase2() {
    override fun foo() {}
}
