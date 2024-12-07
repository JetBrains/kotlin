// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// MODULE: lib
// JVM_DEFAULT_MODE: all
// FILE: P.java

public interface P {
    default String test() {
        return "OK";
    }
}

// FILE: kotlin.kt

abstract class A : P

interface B : P

// MODULE: main(lib)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt
abstract class C : A(), P, B

fun box(): String {
    return object : C() {}.test()
}
