// ISSUE: KT-91218
// NON_CONTEXTUAL_DANGLING_FILE
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
