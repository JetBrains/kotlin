// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: J
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib reads Java declarations from compiled classes, not from sources.
// FILE: J.java

public class J {
    public void bar() {}
}

// FILE: javaMethod.kt

fun test(j: J) = j.bar()
