/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.data;

public class JavaFinalLayer extends JavaConcreteLayer11 implements JavaSideLayerB {
    @Override
    int concreteLayer110(int value) {
        return value + 1;
    }

    @Override
    String concreteLayer111(String text) {
        return text + "!";
    }

    @Override
    long concreteLayer112(long value) {
        return value * 2L;
    }

    @Override
    double concreteLayer113(double value) {
        return value / 2.0;
    }

    @Override
    boolean concreteLayer114(int value) {
        return value % 2 == 0;
    }

    @Override
    CharSequence concreteLayer115(CharSequence text) {
        return text.length() + ":" + text;
    }

    int finalOwn0(int value) {
        return value + 1;
    }

    String finalOwn1(String text) {
        return text + "!";
    }

    long finalOwn2(long value) {
        return value * 2L;
    }

    double finalOwn3(double value) {
        return value / 2.0;
    }

    boolean finalOwn4(int value) {
        return value % 2 == 0;
    }

    CharSequence finalOwn5(CharSequence text) {
        return text.length() + ":" + text;
    }

    static int finalOwnStatic0(int value) {
        return value + 1;
    }

    static String finalOwnStatic1(String text) {
        return text + "!";
    }

    static long finalOwnStatic2(long value) {
        return value * 2L;
    }

    static double finalOwnStatic3(double value) {
        return value / 2.0;
    }

    static boolean finalOwnStatic4(int value) {
        return value % 2 == 0;
    }

    static CharSequence finalOwnStatic5(CharSequence text) {
        return text.length() + ":" + text;
    }
}
