// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: ExtendsKCWithT.java

public class ExtendsKCWithT extends KC {
    public static String bar() {
        return new ExtendsKCWithT().foo();
    }
}

// FILE: KC.kt

// KT-3407 Implementing (in Java) an abstract Kotlin class that implements a trait does not respect trait method definition

interface  T {
    fun foo() = "OK"
}

abstract class KC: T {}

fun box() = ExtendsKCWithT.bar()
