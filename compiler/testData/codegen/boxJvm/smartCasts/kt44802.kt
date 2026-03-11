// TARGET_BACKEND: JVM
// ISSUE: KT-44802

// FILE: foo/Base.java
package foo;

public interface Base {
    String foo();
}

// FILE: foo/PackagePrivateInterface.java
package foo;

interface PackagePrivateInterface extends Base {}

// FILE: foo/A.java
package foo;

public class A implements PackagePrivateInterface {
    public String foo() { return "OK"; }
}

// FILE: foo/B.java
package foo;

public class B implements PackagePrivateInterface {
    public String foo() { return "B"; }
}

// FILE: foo/C.java
package foo;

// FILE: main.kt
package bar

import foo.Base
import foo.A
import foo.B

fun testSmartcast(x: Base): String {
    if (x !is A && x !is B) return "fail"
    return x.foo()
}

fun box() = testSmartcast(A())

