// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// ALLOW_KOTLIN_PACKAGE

// MODULE: sandbox
// FILE: sandbox/module-info.java
import org.jspecify.nullness.NullMarked;

@NullMarked
module sandbox {
    requires java9_annotations;
    exports test;
}

// FILE: sandbox/test/package-info.java
package test;

// FILE: sandbox/test/Foo.java
package test;

class Foo {}

// MODULE: sandbox2
// FILE: sandbox2/module-info.java
module sandbox2 {
    requires java9_annotations;
    exports test;
}

// FILE: sandbox2/test/package-info.java
package test;

// FILE: sandbox2/test/Test.java
package test;

public class Test {
    public void foo(Integer x) {}
}

// FILE: main.kt
import test.Test

fun main(x: Test) {
    x.foo(null)
}
