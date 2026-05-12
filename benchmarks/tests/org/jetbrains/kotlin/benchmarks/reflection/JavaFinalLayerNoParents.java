package org.jetbrains.kotlin.benchmarks.reflection;

public class JavaFinalLayerNoParents {

    long concreteLayer10(int value, long extra) {
        return value * extra;
    }

    String concreteLayer11(String text) {
        return text + "-final";
    }

    boolean finalOwn0(int value) {
        return value % 2 == 0;
    }

    int finalOwn1(String text) {
        return text.length();
    }

    static boolean finalOwnStatic0(int value) {
        return value % 2 == 0;
    }
}
