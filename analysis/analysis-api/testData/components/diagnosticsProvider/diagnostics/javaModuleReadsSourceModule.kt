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
    requires moduleA;
}

// FILE: usage.kt
import a.A

fun usage() {
    A()
}
