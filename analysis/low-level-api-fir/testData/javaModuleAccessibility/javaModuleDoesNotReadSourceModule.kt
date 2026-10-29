// RUN_PIPELINE_TILL: FRONTEND
// JDK_KIND: FULL_JDK_11
// MOCK_MODULE_DOES_NOT_READ_SOURCE

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
    <!JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE!>A<!>()
}
