// ORIGINAL: /compiler/testData/diagnostics/tests/javac/inheritance/InheritanceAmbiguity.fir.kt
// WITH_STDLIB
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

// FILE: a/y.java
package a;

public class y extends x implements i {

    public Z getZ() { return null; }

}

// FILE: test.kt
package a

fun test() = y().getZ()

fun box() = "OK"
