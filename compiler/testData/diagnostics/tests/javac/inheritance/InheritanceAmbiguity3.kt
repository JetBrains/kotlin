// FIR_IDENTICAL
// FILE: a/i.java
package a;

public interface i {
    public class Z {}
}

// FILE: a/i2.java
package a;

public interface i2 {
    public class Z {}
}

// FILE: a/x.java
package a;

public class x implements i, i2 {
    public Z getZ() { return null; }
}

// FILE: test.kt
package a

fun test() = x().<!MISSING_DEPENDENCY_CLASS!>getZ<!>()
