/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.data;

public class MixedFinalLayerJava extends MixedConcreteLayerKotlin11 implements MixedSideLayerJavaB {
    @Override
    public int concreteLayer110(int value) {
        return value + 1;
    }

    @Override
    public String concreteLayer111(String text) {
        return text + "!";
    }

    @Override
    public long concreteLayer112(long value) {
        return value * 2L;
    }

    @Override
    public double concreteLayer113(double value) {
        return value / 2.0;
    }

    @Override
    public boolean concreteLayer114(int value) {
        return value % 2 == 0;
    }

    @Override
    public CharSequence concreteLayer115(CharSequence text) {
        return text.length() + ":" + text;
    }

    public int finalOwn0(int value) {
        return value + 1;
    }

    public String finalOwn1(String text) {
        return text + "!";
    }

    public long finalOwn2(long value) {
        return value * 2L;
    }

    public double finalOwn3(double value) {
        return value / 2.0;
    }

    public boolean finalOwn4(int value) {
        return value % 2 == 0;
    }

    public CharSequence finalOwn5(CharSequence text) {
        return text.length() + ":" + text;
    }

    public static int finalOwnStatic0(int value) {
        return value + 1;
    }

    public static String finalOwnStatic1(String text) {
        return text + "!";
    }

    public static long finalOwnStatic2(long value) {
        return value * 2L;
    }

    public static double finalOwnStatic3(double value) {
        return value / 2.0;
    }

    public static boolean finalOwnStatic4(int value) {
        return value % 2 == 0;
    }

    public static CharSequence finalOwnStatic5(CharSequence text) {
        return text.length() + ":" + text;
    }

    @Override
    public int concreteLayer000(int value) {
        return 0;
    }

    @Override
    public String concreteLayer001(String text) {
        return "";
    }

    @Override
    public long concreteLayer002(long value) {
        return 0;
    }

    @Override
    public double concreteLayer003(double value) {
        return 0;
    }

    @Override
    public boolean concreteLayer004(int value) {
        return false;
    }

    @Override
    public CharSequence concreteLayer005(CharSequence text) {
        return null;
    }
}
