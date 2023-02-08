// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// CHECK_BYTECODE_TEXT
// FILE: a/VeryBase.java

package a;

class VeryBase {
    public String foo = "OK";
}

// FILE: a/Base.java

package a;

public class Base extends VeryBase {
}

// FILE: b/Intermediate.java

package b;

class Intermediate extends a.Base {
}

// FILE: box.kt

package b

private class Final : Intermediate() {
    private val foo = "FAIL"
}

fun box(): String =
    Final().foo

// 1 GETFIELD b/Intermediate.foo
