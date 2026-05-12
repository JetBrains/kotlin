/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.reflection;

interface JavaBaseContract {
    int abstractBase0(int value);

    String abstractBase1(String text);

    default int implementedBase0(int value) {
        return value + 1;
    }

    default String implementedBase1(String text) {
        return new StringBuilder(text).reverse().toString();
    }
}

interface JavaMidContractA extends JavaBaseContract {
    long abstractMidA0(long value);

    CharSequence abstractMidA1(CharSequence text);

    @Override
    default int implementedBase0(int value) {
        return value * 2;
    }

    default long implementedMidA0(long value) {
        return value * value;
    }
}

interface JavaMidContractB extends JavaBaseContract {
    double abstractMidB0(double value);

    @Override
    default String implementedBase1(String text) {
        return text.toUpperCase();
    }

    default double implementedMidB0(double value) {
        return value / 2.0;
    }
}

abstract class JavaAbstractLayer0 implements JavaMidContractA {
    abstract String abstractLayer0(String value);

    @Override
    public String abstractBase1(String text) {
        return "layer0:" + text;
    }

    @Override
    public CharSequence abstractMidA1(CharSequence text) {
        return text.length() + ":" + text;
    }

    String implementedLayer0(String value) {
        return value.trim();
    }
}

abstract class JavaAbstractLayer1 extends JavaAbstractLayer0 implements JavaMidContractB {
    abstract String abstractLayer1(int value, String text);

    @Override
    public int abstractBase0(int value) {
        return value - 1;
    }

    @Override
    public double abstractMidB0(double value) {
        return value + 42.0;
    }

    String implementedLayer1(int value, String text) {
        return value + "-" + text;
    }
}

class JavaConcreteLayer0 extends JavaAbstractLayer1 {
    @Override
    public long abstractMidA0(long value) {
        return value + 10;
    }

    @Override
    String abstractLayer0(String value) {
        return value.toLowerCase();
    }

    @Override
    String abstractLayer1(int value, String text) {
        if (value <= 0) return "";

        StringBuilder result = new StringBuilder(text.length() * value);
        for (int i = 0; i < value; i++) {
            result.append(text);
        }
        return result.toString();
    }

    public String concreteLayer00(String value) {
        return value + "!";
    }

    public int concreteLayer01(int value) {
        return value * 3;
    }
}

interface JavaSideLayerA {
    default String sideLayer00(String value) {
        return "[" + value + "]";
    }

    default int sideLayerA11(int value) {
        return value;
    }

    default long sideLayer10(int value, long extra) {
        return value + extra;
    }

    default String sideLayerA11(String text) {
        return text.startsWith("x") ? text.substring(1) : text;
    }
}

interface JavaSideLayerB {
    public String concreteLayer00(String value);

    public int concreteLayer01(int value);

    default int sideLayerB11(int value) {
        return value;
    }


    default String sideLayerB11(String text) {
        return text.startsWith("x") ? text.substring(1) : text;
    }
}

class JavaConcreteLayer1 extends JavaConcreteLayer0 implements JavaSideLayerB, JavaSideLayerA {

    long concreteLayer10(int value, long extra) {
        return value + extra;
    }

    String concreteLayer11(String text) {
        return text.startsWith("x") ? text.substring(1) : text;
    }
}

class JavaFinalLayer extends JavaConcreteLayer1 implements JavaSideLayerB {
    @Override
    long concreteLayer10(int value, long extra) {
        return value * extra;
    }

    @Override
    String concreteLayer11(String text) {
        return text + "-final";
    }

    boolean finalOwn0(int value) {
        return value % 2 == 0;
    }

    static boolean finalOwnStatic0(int value) {
        return value % 2 == 0;
    }

    int finalOwn1(String text) {
        return text.length();
    }

    static int finalOwnStatic1(String text) {
        return text.length();
    }
}

class JavaFinalLayerNoDeclaredMembers extends JavaConcreteLayer1 {
}
