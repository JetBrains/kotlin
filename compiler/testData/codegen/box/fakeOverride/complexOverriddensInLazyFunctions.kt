// TARGET_BACKEND: JVM_IR
// DUMP_IR
// DUMP_EXTERNAL_CLASS: JavaInterface

// FILE: base.kt
interface A {
    fun foo()
    fun bar()
}

interface B : A

interface C : A

interface D {
    fun bar()
}

// FILE: JavaInterface.java

public interface JavaInterface extends B, C, D {
    public void foo();
    public void bar();
}

// FILE: main.kt

fun test(x: JavaInterface) {
    x.foo()
    x.bar()
}

fun box(): String = "OK"
