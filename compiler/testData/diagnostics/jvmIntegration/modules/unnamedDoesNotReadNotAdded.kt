// Test that although we have moduleA in the module path, it's not in the module graph
// because we did not provide -Xadd-modules=moduleA.

// JDK_KIND: FULL_JDK_11
// FORCE_COMPILE_AS_JAVA_MODULE
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports foo;
}

// FILE: foo/Foo.java
package foo;

public class Foo {}

// MODULE: moduleB(moduleA)
// FILE: usage.kt
import <!UNRESOLVED_REFERENCE!>foo<!>.Foo

fun usage() {
    <!UNRESOLVED_REFERENCE!>Foo<!>()
}
