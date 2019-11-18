// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

import java.util.List;

public class J {
    void simple() {}

    void objectTypes(
            Object o, String s, Object[] oo, String[] ss
    ) {}

    void primitives(
            boolean z, char c, byte b, short s, int i, float f, long j, double d
    ) {}

    void primitiveArrays(
            boolean[] z, char[] c, byte[] b, short[] s, int[] i, float[] f, long[] j, double[] d
    ) {}

    void multiDimensionalArrays(
            int[][][] i, Cloneable[][][][] c
    ) {}

    void wildcards(
            List<? extends Number> l, List<? super Cloneable> m
    ) {}
}

// FILE: K.kt

import kotlin.reflect.KFunction

// Initiate descriptor computation in reflection to ensure that nothing fails
fun test(f: KFunction<*>) {
    f.parameters
}

fun box(): String {
    test(J::simple)
    test(J::objectTypes)
    test(J::primitives)
    test(J::primitiveArrays)
    test(J::multiDimensionalArrays)
    test(J::wildcards)

    return "OK"
}
