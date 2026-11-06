// IGNORE_SELF_MODE
// ISSUE: KT-91218 PREFER_SELF dangling files without a physical original file remain unsupported.
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    requires kotlin.stdlib;
}

// FILE: a/A.java
package a;

public class A {}

// FILE: usage.kt
package usage

import a.A

fun usage() {
    A()
}
