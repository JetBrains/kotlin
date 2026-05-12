/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.data

/*
 * Kotlin half of the alternating Kotlin/Java member hierarchy.
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

internal interface MixedBaseContractKotlin {
    fun abstractBase0(value: Int): Int

    fun abstractBase1(text: String): String

    fun abstractBase2(value: Long): Long

    fun abstractBase3(value: Double): Double

    fun abstractBase4(value: Int): Boolean

    fun abstractBase5(text: CharSequence): CharSequence

    fun implementedBase0(value: Int): Int = value + 1

    fun implementedBase1(text: String): String = text + "!"

    fun implementedBase2(value: Long): Long = value * 2L

    fun implementedBase3(value: Double): Double = value / 2.0

    fun implementedBase4(value: Int): Boolean = value % 2 == 0

    fun implementedBase5(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal interface MixedMidContractKotlinB : MixedBaseContractKotlin {
    fun abstractMidB0(value: Int): Int

    fun abstractMidB1(text: String): String

    fun abstractMidB2(value: Long): Long

    override fun implementedBase1(text: String): String = text + "!"

    fun implementedMidB0(value: Double): Double = value / 2.0

    fun implementedMidB1(value: Int): Boolean = value % 2 == 0

    fun implementedMidB2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal interface MixedMidContractKotlinD : MixedBaseContractKotlin {
    fun abstractMidD0(value: Int): Int

    fun abstractMidD1(text: String): String

    fun abstractMidD2(value: Long): Long

    override fun implementedBase3(value: Double): Double = value / 2.0

    fun implementedMidD0(value: Double): Double = value / 2.0

    fun implementedMidD1(value: Int): Boolean = value % 2 == 0

    fun implementedMidD2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal interface MixedMidContractKotlinF : MixedBaseContractKotlin {
    fun abstractMidF0(value: Int): Int

    fun abstractMidF1(text: String): String

    fun abstractMidF2(value: Long): Long

    override fun implementedBase5(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    fun implementedMidF0(value: Double): Double = value / 2.0

    fun implementedMidF1(value: Int): Boolean = value % 2 == 0

    fun implementedMidF2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal abstract class MixedAbstractLayerKotlin1 : MixedAbstractLayerJava0(), MixedMidContractKotlinB {
    abstract fun abstractLayer010(value: Int): Int

    abstract fun abstractLayer011(text: String): String

    abstract fun abstractLayer012(value: Long): Long

    override fun abstractMidA0(value: Int): Int = value + 1

    override fun abstractMidA1(text: String): String = text + "!"

    override fun abstractMidA2(value: Long): Long = value * 2L

    override fun abstractLayer000(value: Int): Int = value + 1

    override fun abstractLayer001(text: String): String = text + "!"

    override fun abstractLayer002(value: Long): Long = value * 2L

    open fun implementedLayer010(value: Double): Double = value / 2.0

    open fun implementedLayer011(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer012(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal abstract class MixedAbstractLayerKotlin3 : MixedAbstractLayerJava2(), MixedMidContractKotlinD {
    abstract fun abstractLayer030(value: Int): Int

    abstract fun abstractLayer031(text: String): String

    abstract fun abstractLayer032(value: Long): Long

    override fun abstractMidC0(value: Int): Int = value + 1

    override fun abstractMidC1(text: String): String = text + "!"

    override fun abstractMidC2(value: Long): Long = value * 2L

    override fun abstractLayer020(value: Int): Int = value + 1

    override fun abstractLayer021(text: String): String = text + "!"

    override fun abstractLayer022(value: Long): Long = value * 2L

    open fun implementedLayer030(value: Double): Double = value / 2.0

    open fun implementedLayer031(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer032(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal abstract class MixedAbstractLayerKotlin5 : MixedAbstractLayerJava4(), MixedMidContractKotlinF {
    abstract fun abstractLayer050(value: Int): Int

    abstract fun abstractLayer051(text: String): String

    abstract fun abstractLayer052(value: Long): Long

    override fun abstractMidE0(value: Int): Int = value + 1

    override fun abstractMidE1(text: String): String = text + "!"

    override fun abstractMidE2(value: Long): Long = value * 2L

    override fun abstractLayer040(value: Int): Int = value + 1

    override fun abstractLayer041(text: String): String = text + "!"

    override fun abstractLayer042(value: Long): Long = value * 2L

    open fun implementedLayer050(value: Double): Double = value / 2.0

    open fun implementedLayer051(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer052(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal abstract class MixedAbstractLayerKotlin7 : MixedAbstractLayerJava6() {
    abstract fun abstractLayer070(value: Int): Int

    abstract fun abstractLayer071(text: String): String

    abstract fun abstractLayer072(value: Long): Long

    override fun abstractLayer060(value: Int): Int = value + 1

    override fun abstractLayer061(text: String): String = text + "!"

    override fun abstractLayer062(value: Long): Long = value * 2L

    open fun implementedLayer070(value: Double): Double = value / 2.0

    open fun implementedLayer071(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer072(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal abstract class MixedAbstractLayerKotlin9 : MixedAbstractLayerJava8() {
    abstract fun abstractLayer090(value: Int): Int

    abstract fun abstractLayer091(text: String): String

    abstract fun abstractLayer092(value: Long): Long

    override fun abstractLayer080(value: Int): Int = value + 1

    override fun abstractLayer081(text: String): String = text + "!"

    override fun abstractLayer082(value: Long): Long = value * 2L

    open fun implementedLayer090(value: Double): Double = value / 2.0

    open fun implementedLayer091(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer092(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal abstract class MixedAbstractLayerKotlin11 : MixedAbstractLayerJava10() {
    abstract fun abstractLayer110(value: Int): Int

    abstract fun abstractLayer111(text: String): String

    abstract fun abstractLayer112(value: Long): Long

    override fun abstractLayer100(value: Int): Int = value + 1

    override fun abstractLayer101(text: String): String = text + "!"

    override fun abstractLayer102(value: Long): Long = value * 2L

    open fun implementedLayer110(value: Double): Double = value / 2.0

    open fun implementedLayer111(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer112(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal open class MixedConcreteLayerKotlin1 : MixedConcreteLayerJava0(), MixedSideLayerJavaB {
    open fun concreteLayer010(value: Int): Int = value + 1

    open fun concreteLayer011(text: String): String = text + "!"

    open fun concreteLayer012(value: Long): Long = value * 2L

    open fun concreteLayer013(value: Double): Double = value / 2.0

    open fun concreteLayer014(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer015(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal open class MixedConcreteLayerKotlin3 : MixedConcreteLayerJava2(), MixedSideLayerJavaD {
    open fun concreteLayer030(value: Int): Int = value + 1

    open fun concreteLayer031(text: String): String = text + "!"

    open fun concreteLayer032(value: Long): Long = value * 2L

    open fun concreteLayer033(value: Double): Double = value / 2.0

    open fun concreteLayer034(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer035(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal open class MixedConcreteLayerKotlin5 : MixedConcreteLayerJava4(), MixedSideLayerJavaF {
    open fun concreteLayer050(value: Int): Int = value + 1

    open fun concreteLayer051(text: String): String = text + "!"

    open fun concreteLayer052(value: Long): Long = value * 2L

    open fun concreteLayer053(value: Double): Double = value / 2.0

    open fun concreteLayer054(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer055(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal open class MixedConcreteLayerKotlin7 : MixedConcreteLayerJava6() {
    open fun concreteLayer070(value: Int): Int = value + 1

    open fun concreteLayer071(text: String): String = text + "!"

    open fun concreteLayer072(value: Long): Long = value * 2L

    open fun concreteLayer073(value: Double): Double = value / 2.0

    open fun concreteLayer074(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer075(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal open class MixedConcreteLayerKotlin9 : MixedConcreteLayerJava8() {
    open fun concreteLayer090(value: Int): Int = value + 1

    open fun concreteLayer091(text: String): String = text + "!"

    open fun concreteLayer092(value: Long): Long = value * 2L

    open fun concreteLayer093(value: Double): Double = value / 2.0

    open fun concreteLayer094(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer095(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal open class MixedConcreteLayerKotlin11 : MixedConcreteLayerJava10() {
    open fun concreteLayer110(value: Int): Int = value + 1

    open fun concreteLayer111(text: String): String = text + "!"

    open fun concreteLayer112(value: Long): Long = value * 2L

    open fun concreteLayer113(value: Double): Double = value / 2.0

    open fun concreteLayer114(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer115(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

internal interface MixedSideLayerKotlinA {
    fun sideLayerA0(value: Int): Int = value + 1

    fun sideLayerA1(text: String): String = text + "!"

    fun sideLayerA2(value: Long): Long = value * 2L

    fun sideLayerAOverload(value: Int): Int = value

    fun sideLayerAOverload(text: String): String = text
}

internal interface MixedSideLayerKotlinC {
    fun sideLayerC0(value: Int): Int = value + 1

    fun sideLayerC1(text: String): String = text + "!"

    fun sideLayerC2(value: Long): Long = value * 2L

    fun sideLayerCOverload(value: Int): Int = value

    fun sideLayerCOverload(text: String): String = text
}

internal interface MixedSideLayerKotlinE {
    fun sideLayerE0(value: Int): Int = value + 1

    fun sideLayerE1(text: String): String = text + "!"

    fun sideLayerE2(value: Long): Long = value * 2L

    fun sideLayerEOverload(value: Int): Int = value

    fun sideLayerEOverload(text: String): String = text
}

internal class MixedFinalLayerKotlin : MixedConcreteLayerKotlin11(), MixedSideLayerJavaB {
    override fun concreteLayer110(value: Int): Int = value + 1

    override fun concreteLayer111(text: String): String = text + "!"

    override fun concreteLayer112(value: Long): Long = value * 2L

    override fun concreteLayer113(value: Double): Double = value / 2.0

    override fun concreteLayer114(value: Int): Boolean = value % 2 == 0

    override fun concreteLayer115(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    fun finalOwn0(value: Int): Int = value + 1

    fun finalOwn1(text: String): String = text + "!"

    fun finalOwn2(value: Long): Long = value * 2L

    fun finalOwn3(value: Double): Double = value / 2.0

    fun finalOwn4(value: Int): Boolean = value % 2 == 0

    fun finalOwn5(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    // MixedSideLayerJavaB re-declares these as abstract; re-implemented here to match
    // MixedFinalLayerJava, which does the same. Stub bodies mirror the Java side.
    override fun concreteLayer000(value: Int): Int = 0

    override fun concreteLayer001(text: String): String = ""

    override fun concreteLayer002(value: Long): Long = 0L

    override fun concreteLayer003(value: Double): Double = 0.0

    override fun concreteLayer004(value: Int): Boolean = false

    override fun concreteLayer005(text: CharSequence): CharSequence = ""

    companion object {
        @JvmStatic
        fun finalOwnStatic0(value: Int): Int = value + 1

        @JvmStatic
        fun finalOwnStatic1(text: String): String = text + "!"

        @JvmStatic
        fun finalOwnStatic2(value: Long): Long = value * 2L

        @JvmStatic
        fun finalOwnStatic3(value: Double): Double = value / 2.0

        @JvmStatic
        fun finalOwnStatic4(value: Int): Boolean = value % 2 == 0

        @JvmStatic
        fun finalOwnStatic5(text: CharSequence): CharSequence = text.length.toString() + ":" + text
    }
}
