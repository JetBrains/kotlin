/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection;

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
