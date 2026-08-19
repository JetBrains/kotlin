/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection;

/*
 * Java half of the alternating Kotlin/Java member hierarchy.
 *
 * Mirrors the shape of JavaClassMembersHierarchy.java (generated - keep the structure if you
 * edit it by hand): a root interface, 6 sibling mid contracts, 12 abstract layers,
 * 12 concrete layers, 6 side interfaces and a leaf. All members are public because Kotlin
 * has no package-private visibility.
 * Languages alternate down the class chain, so `MixedFinalLayerJava` (Java) and `MixedFinalLayerKotlin`
 * (Kotlin, one level deeper) each sit under a fully alternating chain of supertypes.
 */

interface MixedMidContractA extends MixedBaseContract {
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

interface MixedMidContractC extends MixedBaseContract {
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

interface MixedMidContractE extends MixedBaseContract {
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

abstract class MixedAbstractLayer0 implements MixedMidContractA {
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

abstract class MixedAbstractLayer2 extends MixedAbstractLayer1 implements MixedMidContractC {
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

abstract class MixedAbstractLayer4 extends MixedAbstractLayer3 implements MixedMidContractE {
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

abstract class MixedAbstractLayer6 extends MixedAbstractLayer5 {
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

abstract class MixedAbstractLayer8 extends MixedAbstractLayer7 {
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

abstract class MixedAbstractLayer10 extends MixedAbstractLayer9 {
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

class MixedConcreteLayer0 extends MixedAbstractLayer11 implements MixedSideLayerA {
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

class MixedConcreteLayer2 extends MixedConcreteLayer1 implements MixedSideLayerC {
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

class MixedConcreteLayer4 extends MixedConcreteLayer3 implements MixedSideLayerE {
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

class MixedConcreteLayer6 extends MixedConcreteLayer5 {
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

class MixedConcreteLayer8 extends MixedConcreteLayer7 {
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

class MixedConcreteLayer10 extends MixedConcreteLayer9 {
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

interface MixedSideLayerB {
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

interface MixedSideLayerD {
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

interface MixedSideLayerF {
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

class MixedFinalLayerJava extends MixedConcreteLayer11 implements MixedSideLayerB {
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
}
