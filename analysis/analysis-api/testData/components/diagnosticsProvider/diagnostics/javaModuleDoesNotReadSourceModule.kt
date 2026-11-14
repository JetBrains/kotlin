// IGNORE_SELF_MODE
// ISSUE: KT-91218 PREFER_SELF dangling files without a physical original file remain unsupported.
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports a;
}

// FILE: a/A.java
package a;

public class A {}

// MODULE: moduleB(moduleA)
// FILE: module-info.java
module moduleB {
    requires kotlin.stdlib;
}

// FILE: usage.kt
import a.A

fun usage() {
    A()
}
