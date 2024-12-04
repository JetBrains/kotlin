// FIR_IDENTICAL
// JDK_KIND: FULL_JDK_11
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports foo;
}

// FILE: foo/Foo.java
package foo;

public class Foo {}

// MODULE: moduleB(moduleA)
// FILE: module-info.java
module moduleB {
    requires moduleA;

    requires kotlin.stdlib;
}

// FILE: usage.kt
import foo.Foo

fun usage() {
    Foo()
}
