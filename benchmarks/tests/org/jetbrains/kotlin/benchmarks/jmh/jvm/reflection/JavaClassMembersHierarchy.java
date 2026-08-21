/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection;

/*
 * Deep and wide inheritance hierarchy used to benchmark `KClass.members`.
 *
 * Shape (generated, keep the structure if you edit it by hand):
 *   JavaBaseContract            - root interface, 6 abstract + 6 default methods
 *   JavaMidContractA..F         - 6 sibling interfaces extending the root; each overrides one
 *                                 distinct root default (so no default-method diamonds arise)
 *   JavaAbstractLayer0..11      - 12 abstract classes, each mixing in one mid contract while they
 *                                 last, declaring new abstract members and implementing inherited ones
 *   JavaConcreteLayer0..11      - 12 concrete classes; layer 0 implements every member still abstract,
 *                                 and each layer mixes in one side interface
 *   JavaSideLayerA..F           - 6 sibling interfaces of default methods, incl. overload pairs
 *   JavaFinalLayer              - leaf: overrides, own instance members and statics
 */

interface JavaBaseContract {
    int abstractBase0(int value);

    String abstractBase1(String text);

    long abstractBase2(long value);

    double abstractBase3(double value);

    boolean abstractBase4(int value);

    CharSequence abstractBase5(CharSequence text);

    default int implementedBase0(int value) {
        return value + 1;
    }

    default String implementedBase1(String text) {
        return text + "!";
    }

    default long implementedBase2(long value) {
        return value * 2L;
    }

    default double implementedBase3(double value) {
        return value / 2.0;
    }

    default boolean implementedBase4(int value) {
        return value % 2 == 0;
    }

    default CharSequence implementedBase5(CharSequence text) {
        return text.length() + ":" + text;
    }
}

interface JavaMidContractA extends JavaBaseContract {
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

interface JavaMidContractB extends JavaBaseContract {
    int abstractMidB0(int value);

    String abstractMidB1(String text);

    long abstractMidB2(long value);

    @Override
    default String implementedBase1(String text) {
        return text + "!";
    }

    default double implementedMidB0(double value) {
        return value / 2.0;
    }

    default boolean implementedMidB1(int value) {
        return value % 2 == 0;
    }

    default CharSequence implementedMidB2(CharSequence text) {
        return text.length() + ":" + text;
    }
}

interface JavaMidContractC extends JavaBaseContract {
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

interface JavaMidContractD extends JavaBaseContract {
    int abstractMidD0(int value);

    String abstractMidD1(String text);

    long abstractMidD2(long value);

    @Override
    default double implementedBase3(double value) {
        return value / 2.0;
    }

    default double implementedMidD0(double value) {
        return value / 2.0;
    }

    default boolean implementedMidD1(int value) {
        return value % 2 == 0;
    }

    default CharSequence implementedMidD2(CharSequence text) {
        return text.length() + ":" + text;
    }
}

interface JavaMidContractE extends JavaBaseContract {
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

interface JavaMidContractF extends JavaBaseContract {
    int abstractMidF0(int value);

    String abstractMidF1(String text);

    long abstractMidF2(long value);

    @Override
    default CharSequence implementedBase5(CharSequence text) {
        return text.length() + ":" + text;
    }

    default double implementedMidF0(double value) {
        return value / 2.0;
    }

    default boolean implementedMidF1(int value) {
        return value % 2 == 0;
    }

    default CharSequence implementedMidF2(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer0 implements JavaMidContractA {
    abstract int abstractLayer000(int value);

    abstract String abstractLayer001(String text);

    abstract long abstractLayer002(long value);

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

    double implementedLayer000(double value) {
        return value / 2.0;
    }

    boolean implementedLayer001(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer002(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer1 extends JavaAbstractLayer0 implements JavaMidContractB {
    abstract int abstractLayer010(int value);

    abstract String abstractLayer011(String text);

    abstract long abstractLayer012(long value);

    @Override
    public int abstractMidA0(int value) {
        return value + 1;
    }

    @Override
    public String abstractMidA1(String text) {
        return text + "!";
    }

    @Override
    public long abstractMidA2(long value) {
        return value * 2L;
    }

    @Override
    int abstractLayer000(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer001(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer002(long value) {
        return value * 2L;
    }

    double implementedLayer010(double value) {
        return value / 2.0;
    }

    boolean implementedLayer011(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer012(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer2 extends JavaAbstractLayer1 implements JavaMidContractC {
    abstract int abstractLayer020(int value);

    abstract String abstractLayer021(String text);

    abstract long abstractLayer022(long value);

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
    int abstractLayer010(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer011(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer012(long value) {
        return value * 2L;
    }

    double implementedLayer020(double value) {
        return value / 2.0;
    }

    boolean implementedLayer021(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer022(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer3 extends JavaAbstractLayer2 implements JavaMidContractD {
    abstract int abstractLayer030(int value);

    abstract String abstractLayer031(String text);

    abstract long abstractLayer032(long value);

    @Override
    public int abstractMidC0(int value) {
        return value + 1;
    }

    @Override
    public String abstractMidC1(String text) {
        return text + "!";
    }

    @Override
    public long abstractMidC2(long value) {
        return value * 2L;
    }

    @Override
    int abstractLayer020(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer021(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer022(long value) {
        return value * 2L;
    }

    double implementedLayer030(double value) {
        return value / 2.0;
    }

    boolean implementedLayer031(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer032(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer4 extends JavaAbstractLayer3 implements JavaMidContractE {
    abstract int abstractLayer040(int value);

    abstract String abstractLayer041(String text);

    abstract long abstractLayer042(long value);

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
    int abstractLayer030(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer031(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer032(long value) {
        return value * 2L;
    }

    double implementedLayer040(double value) {
        return value / 2.0;
    }

    boolean implementedLayer041(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer042(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer5 extends JavaAbstractLayer4 implements JavaMidContractF {
    abstract int abstractLayer050(int value);

    abstract String abstractLayer051(String text);

    abstract long abstractLayer052(long value);

    @Override
    public int abstractMidE0(int value) {
        return value + 1;
    }

    @Override
    public String abstractMidE1(String text) {
        return text + "!";
    }

    @Override
    public long abstractMidE2(long value) {
        return value * 2L;
    }

    @Override
    int abstractLayer040(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer041(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer042(long value) {
        return value * 2L;
    }

    double implementedLayer050(double value) {
        return value / 2.0;
    }

    boolean implementedLayer051(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer052(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer6 extends JavaAbstractLayer5 {
    abstract int abstractLayer060(int value);

    abstract String abstractLayer061(String text);

    abstract long abstractLayer062(long value);

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
    int abstractLayer050(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer051(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer052(long value) {
        return value * 2L;
    }

    double implementedLayer060(double value) {
        return value / 2.0;
    }

    boolean implementedLayer061(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer062(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer7 extends JavaAbstractLayer6 {
    abstract int abstractLayer070(int value);

    abstract String abstractLayer071(String text);

    abstract long abstractLayer072(long value);

    @Override
    int abstractLayer060(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer061(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer062(long value) {
        return value * 2L;
    }

    double implementedLayer070(double value) {
        return value / 2.0;
    }

    boolean implementedLayer071(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer072(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer8 extends JavaAbstractLayer7 {
    abstract int abstractLayer080(int value);

    abstract String abstractLayer081(String text);

    abstract long abstractLayer082(long value);

    @Override
    int abstractLayer070(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer071(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer072(long value) {
        return value * 2L;
    }

    double implementedLayer080(double value) {
        return value / 2.0;
    }

    boolean implementedLayer081(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer082(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer9 extends JavaAbstractLayer8 {
    abstract int abstractLayer090(int value);

    abstract String abstractLayer091(String text);

    abstract long abstractLayer092(long value);

    @Override
    int abstractLayer080(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer081(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer082(long value) {
        return value * 2L;
    }

    double implementedLayer090(double value) {
        return value / 2.0;
    }

    boolean implementedLayer091(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer092(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer10 extends JavaAbstractLayer9 {
    abstract int abstractLayer100(int value);

    abstract String abstractLayer101(String text);

    abstract long abstractLayer102(long value);

    @Override
    int abstractLayer090(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer091(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer092(long value) {
        return value * 2L;
    }

    double implementedLayer100(double value) {
        return value / 2.0;
    }

    boolean implementedLayer101(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer102(CharSequence text) {
        return text.length() + ":" + text;
    }
}

abstract class JavaAbstractLayer11 extends JavaAbstractLayer10 {
    abstract int abstractLayer110(int value);

    abstract String abstractLayer111(String text);

    abstract long abstractLayer112(long value);

    @Override
    int abstractLayer100(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer101(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer102(long value) {
        return value * 2L;
    }

    double implementedLayer110(double value) {
        return value / 2.0;
    }

    boolean implementedLayer111(int value) {
        return value % 2 == 0;
    }

    CharSequence implementedLayer112(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer0 extends JavaAbstractLayer11 implements JavaSideLayerA {
    @Override
    int abstractLayer110(int value) {
        return value + 1;
    }

    @Override
    String abstractLayer111(String text) {
        return text + "!";
    }

    @Override
    long abstractLayer112(long value) {
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

class JavaConcreteLayer1 extends JavaConcreteLayer0 implements JavaSideLayerB {
    int concreteLayer010(int value) {
        return value + 1;
    }

    String concreteLayer011(String text) {
        return text + "!";
    }

    long concreteLayer012(long value) {
        return value * 2L;
    }

    double concreteLayer013(double value) {
        return value / 2.0;
    }

    boolean concreteLayer014(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer015(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer2 extends JavaConcreteLayer1 implements JavaSideLayerC {
    int concreteLayer020(int value) {
        return value + 1;
    }

    String concreteLayer021(String text) {
        return text + "!";
    }

    long concreteLayer022(long value) {
        return value * 2L;
    }

    double concreteLayer023(double value) {
        return value / 2.0;
    }

    boolean concreteLayer024(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer025(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer3 extends JavaConcreteLayer2 implements JavaSideLayerD {
    int concreteLayer030(int value) {
        return value + 1;
    }

    String concreteLayer031(String text) {
        return text + "!";
    }

    long concreteLayer032(long value) {
        return value * 2L;
    }

    double concreteLayer033(double value) {
        return value / 2.0;
    }

    boolean concreteLayer034(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer035(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer4 extends JavaConcreteLayer3 implements JavaSideLayerE {
    int concreteLayer040(int value) {
        return value + 1;
    }

    String concreteLayer041(String text) {
        return text + "!";
    }

    long concreteLayer042(long value) {
        return value * 2L;
    }

    double concreteLayer043(double value) {
        return value / 2.0;
    }

    boolean concreteLayer044(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer045(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer5 extends JavaConcreteLayer4 implements JavaSideLayerF {
    int concreteLayer050(int value) {
        return value + 1;
    }

    String concreteLayer051(String text) {
        return text + "!";
    }

    long concreteLayer052(long value) {
        return value * 2L;
    }

    double concreteLayer053(double value) {
        return value / 2.0;
    }

    boolean concreteLayer054(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer055(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer6 extends JavaConcreteLayer5 {
    int concreteLayer060(int value) {
        return value + 1;
    }

    String concreteLayer061(String text) {
        return text + "!";
    }

    long concreteLayer062(long value) {
        return value * 2L;
    }

    double concreteLayer063(double value) {
        return value / 2.0;
    }

    boolean concreteLayer064(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer065(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer7 extends JavaConcreteLayer6 {
    int concreteLayer070(int value) {
        return value + 1;
    }

    String concreteLayer071(String text) {
        return text + "!";
    }

    long concreteLayer072(long value) {
        return value * 2L;
    }

    double concreteLayer073(double value) {
        return value / 2.0;
    }

    boolean concreteLayer074(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer075(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer8 extends JavaConcreteLayer7 {
    int concreteLayer080(int value) {
        return value + 1;
    }

    String concreteLayer081(String text) {
        return text + "!";
    }

    long concreteLayer082(long value) {
        return value * 2L;
    }

    double concreteLayer083(double value) {
        return value / 2.0;
    }

    boolean concreteLayer084(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer085(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer9 extends JavaConcreteLayer8 {
    int concreteLayer090(int value) {
        return value + 1;
    }

    String concreteLayer091(String text) {
        return text + "!";
    }

    long concreteLayer092(long value) {
        return value * 2L;
    }

    double concreteLayer093(double value) {
        return value / 2.0;
    }

    boolean concreteLayer094(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer095(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer10 extends JavaConcreteLayer9 {
    int concreteLayer100(int value) {
        return value + 1;
    }

    String concreteLayer101(String text) {
        return text + "!";
    }

    long concreteLayer102(long value) {
        return value * 2L;
    }

    double concreteLayer103(double value) {
        return value / 2.0;
    }

    boolean concreteLayer104(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer105(CharSequence text) {
        return text.length() + ":" + text;
    }
}

class JavaConcreteLayer11 extends JavaConcreteLayer10 {
    int concreteLayer110(int value) {
        return value + 1;
    }

    String concreteLayer111(String text) {
        return text + "!";
    }

    long concreteLayer112(long value) {
        return value * 2L;
    }

    double concreteLayer113(double value) {
        return value / 2.0;
    }

    boolean concreteLayer114(int value) {
        return value % 2 == 0;
    }

    CharSequence concreteLayer115(CharSequence text) {
        return text.length() + ":" + text;
    }
}

interface JavaSideLayerA {
    default int sideLayerA0(int value) {
        return value + 1;
    }

    default String sideLayerA1(String text) {
        return text + "!";
    }

    default long sideLayerA2(long value) {
        return value * 2L;
    }

    default int sideLayerAOverload(int value) {
        return value;
    }

    default String sideLayerAOverload(String text) {
        return text;
    }
}

interface JavaSideLayerB {
    public int concreteLayer000(int value);

    public String concreteLayer001(String text);

    public long concreteLayer002(long value);

    public double concreteLayer003(double value);

    public boolean concreteLayer004(int value);

    public CharSequence concreteLayer005(CharSequence text);

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

interface JavaSideLayerC {
    default int sideLayerC0(int value) {
        return value + 1;
    }

    default String sideLayerC1(String text) {
        return text + "!";
    }

    default long sideLayerC2(long value) {
        return value * 2L;
    }

    default int sideLayerCOverload(int value) {
        return value;
    }

    default String sideLayerCOverload(String text) {
        return text;
    }
}

interface JavaSideLayerD {
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

interface JavaSideLayerE {
    default int sideLayerE0(int value) {
        return value + 1;
    }

    default String sideLayerE1(String text) {
        return text + "!";
    }

    default long sideLayerE2(long value) {
        return value * 2L;
    }

    default int sideLayerEOverload(int value) {
        return value;
    }

    default String sideLayerEOverload(String text) {
        return text;
    }
}

interface JavaSideLayerF {
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

class JavaFinalLayer extends JavaConcreteLayer11 implements JavaSideLayerB {
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

class JavaFinalLayerNoDeclaredMembers extends JavaConcreteLayer11 {
}
