// SKIP_TXT
// FILE: test/J.java

package test;

@Deprecated
public interface J {
    public String foo(int x);
}

// FILE: K.kt

import test.<!DEPRECATION!>J<!>
