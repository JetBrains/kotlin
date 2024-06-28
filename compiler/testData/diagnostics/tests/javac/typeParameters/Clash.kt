// FIR_IDENTICAL
// FILE: a/x.java
package a;

public class x {
    public class O {}
}

// FILE: a/i.java
package a;

public interface i {
    public class O {}
}

// FILE: a/i2.java
package a;

public interface i2 extends i {
    public O getO();
}

// FILE: a/Test.java
package a;

public class Test extends x implements i2 {
    @Override
    public O getO() { return null; }
}

// FILE: test.kt
package a

fun test() = Test().<!MISSING_DEPENDENCY_CLASS!>getO<!>()
