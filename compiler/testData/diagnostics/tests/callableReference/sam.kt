// FILE: test/GenericSam.java

package test;

public interface GenericSam<T> {
    void invoke(T t);
}

// FILE: test.kt

import test.GenericSam

fun f1() = Runnable::class
fun f2() = Runnable::run
fun f3() = java.lang.Runnable::class
fun f4() = java.lang.Runnable::run

fun f5() = GenericSam::class
fun f6() = GenericSam<*>::invoke
fun f7() = test.GenericSam::class
fun f8() = test.GenericSam<String>::invoke

fun g1() = Runnable {}::class
fun g2() = Runnable {}::run
fun g3() = java.lang.Runnable {}::class
fun g4() = java.lang.Runnable {}::run

fun g5() = GenericSam<String> {}::class
fun g6() = GenericSam<String> {}::invoke
fun g7() = test.GenericSam<String> {}::class
fun g8() = test.GenericSam<String> {}::invoke
