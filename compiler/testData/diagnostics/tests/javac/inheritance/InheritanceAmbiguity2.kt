// FIR_IDENTICAL
// FILE: a/x.java
package a;

public class x {
    public class Z {}
}

// FILE: a/i.java
package a;

public interface i {
    public class Z {}
}

// FILE: a/i2.java
package a;

public interface i2 extends i {}

// FILE: a/y.java
package a;

public class y extends x implements i2 {
    public Z getZ() { return null; }
}

// FILE: test.kt
package a

fun test() = y().<!MISSING_DEPENDENCY_CLASS!>getZ<!>()
