/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.data;

/*
 * Java half of the alternating Kotlin/Java member hierarchy.
 *
 * Mirrors the shape of JavaClassMembersHierarchy.java (generated - keep the structure if you
 * edit it by hand): a root interface, 6 sibling mid contracts, 12 abstract layers,
 * 12 concrete layers, 6 side interfaces and a leaf. All members are public because Kotlin
 * has no package-private visibility.
 * Every type name spells out the language it is written in - role, then `Java` or `Kotlin`, then the
 * index - so a supertype list shows exactly where each link crosses the language boundary.
 * Both halves live in one package: the Java types above the leaves are package-private, and the
 * chain alternates at every link, so a split package would make them invisible to the Kotlin half.
 * The two leaves are siblings, not nested: `MixedFinalLayerJava` and `MixedFinalLayerKotlin` both
 * extend `MixedConcreteLayerKotlin11`, implement `MixedSideLayerJavaB` and declare the same 24
 * members, so the only thing that differs between them is the language the leaf is written in.
 * One consequence: the Kotlin leaf is the single place where two Kotlin classes meet - everywhere
 * else the chain alternates at every link. That is the price of making the pair comparable.
 * The benchmark has to name both, so `MixedFinalLayerJava` is public and therefore lives in its own
 * file - Java allows a public top-level class only in a file of the same name - exactly as
 * `JavaFinalLayer` does. Every other Java type here stays package-private, and the Kotlin ones are
 * `internal`.
 */

interface MixedMidContractJavaA extends MixedBaseContractKotlin {
    int abstractMidA0(int value);

    String abstractMidA1(String text);

    long abstractMidA2(long value);

    @Override
    default int implementedBase0(int value) {
        return value + 1;
    }

    default double implementedMidA0(double value) {
        return value / 2.0;
    }

    default boolean implementedMidA1(int value) {
        return value % 2 == 0;
    }

    default CharSequence implementedMidA2(CharSequence text) {
        return text.length() + ":" + text;
    }
}

interface MixedMidContractJavaC extends MixedBaseContractKotlin {
    int abstractMidC0(int value);

    String abstractMidC1(String text);

    long abstractMidC2(long value);

    @Override
    default long implementedBase2(long value) {
        return value * 2L;
    }

    default double implementedMidC0(double value) {
        return value / 2.0;
    }

    default boolean implementedMidC1(int value) {
        return value % 2 == 0;
    }

    default CharSequence implementedMidC2(CharSequence text) {
        return text.length() + ":" + text;
    }
}

interface MixedMidContractJavaE extends MixedBaseContractKotlin {
    int abstractMidE0(int value);

    String abstractMidE1(String text);

    long abstractMidE2(long value);

    @Override
    default boolean implementedBase4(int value) {
        return value % 2 == 0;
    }

    default double implementedMidE0(double value) {
        return value / 2.0;
    }

    default boolean implementedMidE1(int value) {
        return value % 2 == 0;
    }

    default CharSequence implementedMidE2(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class MixedAbstractLayerJava0 implements MixedMidContractJavaA {
    public abstract int abstractLayer000(int value);

    public abstract String abstractLayer001(String text);

    public abstract long abstractLayer002(long value);

    @Override
    public int abstractBase0(int value) {
        return value + 1;
    }

    @Override
    public String abstractBase1(String text) {
        return text + "!";
    }

    @Override
    public long abstractBase2(long value) {
        return value * 2L;
    }

    @Override
    public double abstractBase3(double value) {
        return value / 2.0;
    }

    @Override
    public boolean abstractBase4(int value) {
        return value % 2 == 0;
    }

    @Override
    public CharSequence abstractBase5(CharSequence text) {
        return text.length() + ":" + text;
    }

    public double implementedLayer000(double value) {
        return value / 2.0;
    }

    public boolean implementedLayer001(int value) {
        return value % 2 == 0;
    }

    public CharSequence implementedLayer002(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class MixedAbstractLayerJava2 extends MixedAbstractLayerKotlin1 implements MixedMidContractJavaC {
    public abstract int abstractLayer020(int value);

    public abstract String abstractLayer021(String text);

    public abstract long abstractLayer022(long value);

    @Override
    public int abstractMidB0(int value) {
        return value + 1;
    }

    @Override
    public String abstractMidB1(String text) {
        return text + "!";
    }

    @Override
    public long abstractMidB2(long value) {
        return value * 2L;
    }

    @Override
    public int abstractLayer010(int value) {
        return value + 1;
    }

    @Override
    public String abstractLayer011(String text) {
        return text + "!";
    }

    @Override
    public long abstractLayer012(long value) {
        return value * 2L;
    }

    public double implementedLayer020(double value) {
        return value / 2.0;
    }

    public boolean implementedLayer021(int value) {
        return value % 2 == 0;
    }

    public CharSequence implementedLayer022(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class MixedAbstractLayerJava4 extends MixedAbstractLayerKotlin3 implements MixedMidContractJavaE {
    public abstract int abstractLayer040(int value);

    public abstract String abstractLayer041(String text);

    public abstract long abstractLayer042(long value);

    @Override
    public int abstractMidD0(int value) {
        return value + 1;
    }

    @Override
    public String abstractMidD1(String text) {
        return text + "!";
    }

    @Override
    public long abstractMidD2(long value) {
        return value * 2L;
    }

    @Override
    public int abstractLayer030(int value) {
        return value + 1;
    }

    @Override
    public String abstractLayer031(String text) {
        return text + "!";
    }

    @Override
    public long abstractLayer032(long value) {
        return value * 2L;
    }

    public double implementedLayer040(double value) {
        return value / 2.0;
    }

    public boolean implementedLayer041(int value) {
        return value % 2 == 0;
    }

    public CharSequence implementedLayer042(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class MixedAbstractLayerJava6 extends MixedAbstractLayerKotlin5 {
    public abstract int abstractLayer060(int value);

    public abstract String abstractLayer061(String text);

    public abstract long abstractLayer062(long value);

    @Override
    public int abstractMidF0(int value) {
        return value + 1;
    }

    @Override
    public String abstractMidF1(String text) {
        return text + "!";
    }

    @Override
    public long abstractMidF2(long value) {
        return value * 2L;
    }

    @Override
    public int abstractLayer050(int value) {
        return value + 1;
    }

    @Override
    public String abstractLayer051(String text) {
        return text + "!";
    }

    @Override
    public long abstractLayer052(long value) {
        return value * 2L;
    }

    public double implementedLayer060(double value) {
        return value / 2.0;
    }

    public boolean implementedLayer061(int value) {
        return value % 2 == 0;
    }

    public CharSequence implementedLayer062(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class MixedAbstractLayerJava8 extends MixedAbstractLayerKotlin7 {
    public abstract int abstractLayer080(int value);

    public abstract String abstractLayer081(String text);

    public abstract long abstractLayer082(long value);

    @Override
    public int abstractLayer070(int value) {
        return value + 1;
    }

    @Override
    public String abstractLayer071(String text) {
        return text + "!";
    }

    @Override
    public long abstractLayer072(long value) {
        return value * 2L;
    }

    public double implementedLayer080(double value) {
        return value / 2.0;
    }

    public boolean implementedLayer081(int value) {
        return value % 2 == 0;
    }

    public CharSequence implementedLayer082(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class MixedAbstractLayerJava10 extends MixedAbstractLayerKotlin9 {
    public abstract int abstractLayer100(int value);

    public abstract String abstractLayer101(String text);

    public abstract long abstractLayer102(long value);

    @Override
    public int abstractLayer090(int value) {
        return value + 1;
    }

    @Override
    public String abstractLayer091(String text) {
        return text + "!";
    }

    @Override
    public long abstractLayer092(long value) {
        return value * 2L;
    }

    public double implementedLayer100(double value) {
        return value / 2.0;
    }

    public boolean implementedLayer101(int value) {
        return value % 2 == 0;
    }

    public CharSequence implementedLayer102(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class MixedConcreteLayerJava0 extends MixedAbstractLayerKotlin11 implements MixedSideLayerKotlinA {
    @Override
    public int abstractLayer110(int value) {
        return value + 1;
    }

    @Override
    public String abstractLayer111(String text) {
        return text + "!";
    }

    @Override
    public long abstractLayer112(long value) {
        return value * 2L;
    }

    public int concreteLayer000(int value) {
        return value + 1;
    }

    public String concreteLayer001(String text) {
        return text + "!";
    }

    public long concreteLayer002(long value) {
        return value * 2L;
    }

    public double concreteLayer003(double value) {
        return value / 2.0;
    }

    public boolean concreteLayer004(int value) {
        return value % 2 == 0;
    }

    public CharSequence concreteLayer005(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class MixedConcreteLayerJava2 extends MixedConcreteLayerKotlin1 implements MixedSideLayerKotlinC {
    public int concreteLayer020(int value) {
        return value + 1;
    }

    public String concreteLayer021(String text) {
        return text + "!";
    }

    public long concreteLayer022(long value) {
        return value * 2L;
    }

    public double concreteLayer023(double value) {
        return value / 2.0;
    }

    public boolean concreteLayer024(int value) {
        return value % 2 == 0;
    }

    public CharSequence concreteLayer025(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class MixedConcreteLayerJava4 extends MixedConcreteLayerKotlin3 implements MixedSideLayerKotlinE {
    public int concreteLayer040(int value) {
        return value + 1;
    }

    public String concreteLayer041(String text) {
        return text + "!";
    }

    public long concreteLayer042(long value) {
        return value * 2L;
    }

    public double concreteLayer043(double value) {
        return value / 2.0;
    }

    public boolean concreteLayer044(int value) {
        return value % 2 == 0;
    }

    public CharSequence concreteLayer045(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class MixedConcreteLayerJava6 extends MixedConcreteLayerKotlin5 {
    public int concreteLayer060(int value) {
        return value + 1;
    }

    public String concreteLayer061(String text) {
        return text + "!";
    }

    public long concreteLayer062(long value) {
        return value * 2L;
    }

    public double concreteLayer063(double value) {
        return value / 2.0;
    }

    public boolean concreteLayer064(int value) {
        return value % 2 == 0;
    }

    public CharSequence concreteLayer065(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class MixedConcreteLayerJava8 extends MixedConcreteLayerKotlin7 {
    public int concreteLayer080(int value) {
        return value + 1;
    }

    public String concreteLayer081(String text) {
        return text + "!";
    }

    public long concreteLayer082(long value) {
        return value * 2L;
    }

    public double concreteLayer083(double value) {
        return value / 2.0;
    }

    public boolean concreteLayer084(int value) {
        return value % 2 == 0;
    }

    public CharSequence concreteLayer085(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class MixedConcreteLayerJava10 extends MixedConcreteLayerKotlin9 {
    public int concreteLayer100(int value) {
        return value + 1;
    }

    public String concreteLayer101(String text) {
        return text + "!";
    }

    public long concreteLayer102(long value) {
        return value * 2L;
    }

    public double concreteLayer103(double value) {
        return value / 2.0;
    }

    public boolean concreteLayer104(int value) {
        return value % 2 == 0;
    }

    public CharSequence concreteLayer105(CharSequence text) {
        return text.length() + ":" + text;
    }
}

interface MixedSideLayerJavaB {
    int concreteLayer000(int value);

    String concreteLayer001(String text);

    long concreteLayer002(long value);

    double concreteLayer003(double value);

    boolean concreteLayer004(int value);

    CharSequence concreteLayer005(CharSequence text);

    default double sideLayerB0(double value) {
        return value / 2.0;
    }

    default boolean sideLayerB1(int value) {
        return value % 2 == 0;
    }

    default CharSequence sideLayerB2(CharSequence text) {
        return text.length() + ":" + text;
    }

    default int sideLayerBOverload(int value) {
        return value;
    }

    default String sideLayerBOverload(String text) {
        return text;
    }
}

interface MixedSideLayerJavaD {
    default double sideLayerD0(double value) {
        return value / 2.0;
    }

    default boolean sideLayerD1(int value) {
        return value % 2 == 0;
    }

    default CharSequence sideLayerD2(CharSequence text) {
        return text.length() + ":" + text;
    }

    default int sideLayerDOverload(int value) {
        return value;
    }

    default String sideLayerDOverload(String text) {
        return text;
    }
}

interface MixedSideLayerJavaF {
    default double sideLayerF0(double value) {
        return value / 2.0;
    }

    default boolean sideLayerF1(int value) {
        return value % 2 == 0;
    }

    default CharSequence sideLayerF2(CharSequence text) {
        return text.length() + ":" + text;
    }

    default int sideLayerFOverload(int value) {
        return value;
    }

    default String sideLayerFOverload(String text) {
        return text;
    }
}
