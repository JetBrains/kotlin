// FIR_IDENTICAL
// JDK_KIND: FULL_JDK_11
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports a;
}

// FILE: a/A.java
package a;

public class A {}

// MODULE: moduleB
// FILE: module-info.java
module moduleB {
    exports b;
}

// FILE: b/B.java
package b;

public class B {}

// MODULE: moduleC
// FILE: module-info.java
module moduleC {
    exports c;
}

// FILE: c/C.java
package c;

public class C {}

// MODULE: moduleD(moduleA, moduleB, moduleC)
// FILE: module-info.java
module moduleD {
    requires moduleA;
    requires moduleB;
    requires moduleC;

    requires kotlin.stdlib;
}

// FILE: usage.kt
import a.*
import b.B
import c.C

fun usage() {
    A()
    B()
    C()
}
