/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "ConvertToStringTemplate")

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.data

/*
 * All-Kotlin member hierarchy. Three leaves mirroring the Java ones: `KotlinFinalLayer`,
 * `KotlinFinalLayerNoDeclaredMembers` and `KotlinFinalLayerNoParents`.
 *
 * Mirrors the shape of JavaClassMembersHierarchy.java (generated - keep the structure if you
 * edit it by hand): a root interface, 6 sibling mid contracts, 12 abstract layers,
 * 12 concrete layers, 6 side interfaces and a leaf. All members are public because Kotlin
 * has no package-private visibility.
 */

interface KotlinBaseContract {
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

interface KotlinMidContractA : KotlinBaseContract {
    fun abstractMidA0(value: Int): Int

    fun abstractMidA1(text: String): String

    fun abstractMidA2(value: Long): Long

    override fun implementedBase0(value: Int): Int = value + 1

    fun implementedMidA0(value: Double): Double = value / 2.0

    fun implementedMidA1(value: Int): Boolean = value % 2 == 0

    fun implementedMidA2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

interface KotlinMidContractB : KotlinBaseContract {
    fun abstractMidB0(value: Int): Int

    fun abstractMidB1(text: String): String

    fun abstractMidB2(value: Long): Long

    override fun implementedBase1(text: String): String = text + "!"

    fun implementedMidB0(value: Double): Double = value / 2.0

    fun implementedMidB1(value: Int): Boolean = value % 2 == 0

    fun implementedMidB2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

interface KotlinMidContractC : KotlinBaseContract {
    fun abstractMidC0(value: Int): Int

    fun abstractMidC1(text: String): String

    fun abstractMidC2(value: Long): Long

    override fun implementedBase2(value: Long): Long = value * 2L

    fun implementedMidC0(value: Double): Double = value / 2.0

    fun implementedMidC1(value: Int): Boolean = value % 2 == 0

    fun implementedMidC2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

interface KotlinMidContractD : KotlinBaseContract {
    fun abstractMidD0(value: Int): Int

    fun abstractMidD1(text: String): String

    fun abstractMidD2(value: Long): Long

    override fun implementedBase3(value: Double): Double = value / 2.0

    fun implementedMidD0(value: Double): Double = value / 2.0

    fun implementedMidD1(value: Int): Boolean = value % 2 == 0

    fun implementedMidD2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

interface KotlinMidContractE : KotlinBaseContract {
    fun abstractMidE0(value: Int): Int

    fun abstractMidE1(text: String): String

    fun abstractMidE2(value: Long): Long

    override fun implementedBase4(value: Int): Boolean = value % 2 == 0

    fun implementedMidE0(value: Double): Double = value / 2.0

    fun implementedMidE1(value: Int): Boolean = value % 2 == 0

    fun implementedMidE2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

interface KotlinMidContractF : KotlinBaseContract {
    fun abstractMidF0(value: Int): Int

    fun abstractMidF1(text: String): String

    fun abstractMidF2(value: Long): Long

    override fun implementedBase5(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    fun implementedMidF0(value: Double): Double = value / 2.0

    fun implementedMidF1(value: Int): Boolean = value % 2 == 0

    fun implementedMidF2(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

abstract class KotlinAbstractLayer0 : KotlinMidContractA {
    abstract fun abstractLayer000(value: Int): Int

    abstract fun abstractLayer001(text: String): String

    abstract fun abstractLayer002(value: Long): Long

    override fun abstractBase0(value: Int): Int = value + 1

    override fun abstractBase1(text: String): String = text + "!"

    override fun abstractBase2(value: Long): Long = value * 2L

    override fun abstractBase3(value: Double): Double = value / 2.0

    override fun abstractBase4(value: Int): Boolean = value % 2 == 0

    override fun abstractBase5(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    open fun implementedLayer000(value: Double): Double = value / 2.0

    open fun implementedLayer001(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer002(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

abstract class KotlinAbstractLayer1 : KotlinAbstractLayer0(), KotlinMidContractB {
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

abstract class KotlinAbstractLayer2 : KotlinAbstractLayer1(), KotlinMidContractC {
    abstract fun abstractLayer020(value: Int): Int

    abstract fun abstractLayer021(text: String): String

    abstract fun abstractLayer022(value: Long): Long

    override fun abstractMidB0(value: Int): Int = value + 1

    override fun abstractMidB1(text: String): String = text + "!"

    override fun abstractMidB2(value: Long): Long = value * 2L

    override fun abstractLayer010(value: Int): Int = value + 1

    override fun abstractLayer011(text: String): String = text + "!"

    override fun abstractLayer012(value: Long): Long = value * 2L

    open fun implementedLayer020(value: Double): Double = value / 2.0

    open fun implementedLayer021(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer022(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

abstract class KotlinAbstractLayer3 : KotlinAbstractLayer2(), KotlinMidContractD {
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

abstract class KotlinAbstractLayer4 : KotlinAbstractLayer3(), KotlinMidContractE {
    abstract fun abstractLayer040(value: Int): Int

    abstract fun abstractLayer041(text: String): String

    abstract fun abstractLayer042(value: Long): Long

    override fun abstractMidD0(value: Int): Int = value + 1

    override fun abstractMidD1(text: String): String = text + "!"

    override fun abstractMidD2(value: Long): Long = value * 2L

    override fun abstractLayer030(value: Int): Int = value + 1

    override fun abstractLayer031(text: String): String = text + "!"

    override fun abstractLayer032(value: Long): Long = value * 2L

    open fun implementedLayer040(value: Double): Double = value / 2.0

    open fun implementedLayer041(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer042(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

abstract class KotlinAbstractLayer5 : KotlinAbstractLayer4(), KotlinMidContractF {
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

abstract class KotlinAbstractLayer6 : KotlinAbstractLayer5() {
    abstract fun abstractLayer060(value: Int): Int

    abstract fun abstractLayer061(text: String): String

    abstract fun abstractLayer062(value: Long): Long

    override fun abstractMidF0(value: Int): Int = value + 1

    override fun abstractMidF1(text: String): String = text + "!"

    override fun abstractMidF2(value: Long): Long = value * 2L

    override fun abstractLayer050(value: Int): Int = value + 1

    override fun abstractLayer051(text: String): String = text + "!"

    override fun abstractLayer052(value: Long): Long = value * 2L

    open fun implementedLayer060(value: Double): Double = value / 2.0

    open fun implementedLayer061(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer062(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

abstract class KotlinAbstractLayer7 : KotlinAbstractLayer6() {
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

abstract class KotlinAbstractLayer8 : KotlinAbstractLayer7() {
    abstract fun abstractLayer080(value: Int): Int

    abstract fun abstractLayer081(text: String): String

    abstract fun abstractLayer082(value: Long): Long

    override fun abstractLayer070(value: Int): Int = value + 1

    override fun abstractLayer071(text: String): String = text + "!"

    override fun abstractLayer072(value: Long): Long = value * 2L

    open fun implementedLayer080(value: Double): Double = value / 2.0

    open fun implementedLayer081(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer082(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

abstract class KotlinAbstractLayer9 : KotlinAbstractLayer8() {
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

abstract class KotlinAbstractLayer10 : KotlinAbstractLayer9() {
    abstract fun abstractLayer100(value: Int): Int

    abstract fun abstractLayer101(text: String): String

    abstract fun abstractLayer102(value: Long): Long

    override fun abstractLayer090(value: Int): Int = value + 1

    override fun abstractLayer091(text: String): String = text + "!"

    override fun abstractLayer092(value: Long): Long = value * 2L

    open fun implementedLayer100(value: Double): Double = value / 2.0

    open fun implementedLayer101(value: Int): Boolean = value % 2 == 0

    open fun implementedLayer102(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

abstract class KotlinAbstractLayer11 : KotlinAbstractLayer10() {
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

open class KotlinConcreteLayer0 : KotlinAbstractLayer11(), KotlinSideLayerA {
    override fun abstractLayer110(value: Int): Int = value + 1

    override fun abstractLayer111(text: String): String = text + "!"

    override fun abstractLayer112(value: Long): Long = value * 2L

    open fun concreteLayer000(value: Int): Int = value + 1

    open fun concreteLayer001(text: String): String = text + "!"

    open fun concreteLayer002(value: Long): Long = value * 2L

    open fun concreteLayer003(value: Double): Double = value / 2.0

    open fun concreteLayer004(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer005(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer1 : KotlinConcreteLayer0(), KotlinSideLayerB {
    open fun concreteLayer010(value: Int): Int = value + 1

    open fun concreteLayer011(text: String): String = text + "!"

    open fun concreteLayer012(value: Long): Long = value * 2L

    open fun concreteLayer013(value: Double): Double = value / 2.0

    open fun concreteLayer014(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer015(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer2 : KotlinConcreteLayer1(), KotlinSideLayerC {
    open fun concreteLayer020(value: Int): Int = value + 1

    open fun concreteLayer021(text: String): String = text + "!"

    open fun concreteLayer022(value: Long): Long = value * 2L

    open fun concreteLayer023(value: Double): Double = value / 2.0

    open fun concreteLayer024(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer025(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer3 : KotlinConcreteLayer2(), KotlinSideLayerD {
    open fun concreteLayer030(value: Int): Int = value + 1

    open fun concreteLayer031(text: String): String = text + "!"

    open fun concreteLayer032(value: Long): Long = value * 2L

    open fun concreteLayer033(value: Double): Double = value / 2.0

    open fun concreteLayer034(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer035(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer4 : KotlinConcreteLayer3(), KotlinSideLayerE {
    open fun concreteLayer040(value: Int): Int = value + 1

    open fun concreteLayer041(text: String): String = text + "!"

    open fun concreteLayer042(value: Long): Long = value * 2L

    open fun concreteLayer043(value: Double): Double = value / 2.0

    open fun concreteLayer044(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer045(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer5 : KotlinConcreteLayer4(), KotlinSideLayerF {
    open fun concreteLayer050(value: Int): Int = value + 1

    open fun concreteLayer051(text: String): String = text + "!"

    open fun concreteLayer052(value: Long): Long = value * 2L

    open fun concreteLayer053(value: Double): Double = value / 2.0

    open fun concreteLayer054(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer055(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer6 : KotlinConcreteLayer5() {
    open fun concreteLayer060(value: Int): Int = value + 1

    open fun concreteLayer061(text: String): String = text + "!"

    open fun concreteLayer062(value: Long): Long = value * 2L

    open fun concreteLayer063(value: Double): Double = value / 2.0

    open fun concreteLayer064(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer065(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer7 : KotlinConcreteLayer6() {
    open fun concreteLayer070(value: Int): Int = value + 1

    open fun concreteLayer071(text: String): String = text + "!"

    open fun concreteLayer072(value: Long): Long = value * 2L

    open fun concreteLayer073(value: Double): Double = value / 2.0

    open fun concreteLayer074(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer075(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer8 : KotlinConcreteLayer7() {
    open fun concreteLayer080(value: Int): Int = value + 1

    open fun concreteLayer081(text: String): String = text + "!"

    open fun concreteLayer082(value: Long): Long = value * 2L

    open fun concreteLayer083(value: Double): Double = value / 2.0

    open fun concreteLayer084(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer085(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer9 : KotlinConcreteLayer8() {
    open fun concreteLayer090(value: Int): Int = value + 1

    open fun concreteLayer091(text: String): String = text + "!"

    open fun concreteLayer092(value: Long): Long = value * 2L

    open fun concreteLayer093(value: Double): Double = value / 2.0

    open fun concreteLayer094(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer095(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer10 : KotlinConcreteLayer9() {
    open fun concreteLayer100(value: Int): Int = value + 1

    open fun concreteLayer101(text: String): String = text + "!"

    open fun concreteLayer102(value: Long): Long = value * 2L

    open fun concreteLayer103(value: Double): Double = value / 2.0

    open fun concreteLayer104(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer105(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

open class KotlinConcreteLayer11 : KotlinConcreteLayer10() {
    open fun concreteLayer110(value: Int): Int = value + 1

    open fun concreteLayer111(text: String): String = text + "!"

    open fun concreteLayer112(value: Long): Long = value * 2L

    open fun concreteLayer113(value: Double): Double = value / 2.0

    open fun concreteLayer114(value: Int): Boolean = value % 2 == 0

    open fun concreteLayer115(text: CharSequence): CharSequence = text.length.toString() + ":" + text
}

interface KotlinSideLayerA {
    fun sideLayerA0(value: Int): Int = value + 1

    fun sideLayerA1(text: String): String = text + "!"

    fun sideLayerA2(value: Long): Long = value * 2L

    fun sideLayerAOverload(value: Int): Int = value

    fun sideLayerAOverload(text: String): String = text
}

interface KotlinSideLayerB {
    fun concreteLayer000(value: Int): Int

    fun concreteLayer001(text: String): String

    fun concreteLayer002(value: Long): Long

    fun concreteLayer003(value: Double): Double

    fun concreteLayer004(value: Int): Boolean

    fun concreteLayer005(text: CharSequence): CharSequence

    fun sideLayerB0(value: Double): Double = value / 2.0

    fun sideLayerB1(value: Int): Boolean = value % 2 == 0

    fun sideLayerB2(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    fun sideLayerBOverload(value: Int): Int = value

    fun sideLayerBOverload(text: String): String = text
}

interface KotlinSideLayerC {
    fun sideLayerC0(value: Int): Int = value + 1

    fun sideLayerC1(text: String): String = text + "!"

    fun sideLayerC2(value: Long): Long = value * 2L

    fun sideLayerCOverload(value: Int): Int = value

    fun sideLayerCOverload(text: String): String = text
}

interface KotlinSideLayerD {
    fun sideLayerD0(value: Double): Double = value / 2.0

    fun sideLayerD1(value: Int): Boolean = value % 2 == 0

    fun sideLayerD2(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    fun sideLayerDOverload(value: Int): Int = value

    fun sideLayerDOverload(text: String): String = text
}

interface KotlinSideLayerE {
    fun sideLayerE0(value: Int): Int = value + 1

    fun sideLayerE1(text: String): String = text + "!"

    fun sideLayerE2(value: Long): Long = value * 2L

    fun sideLayerEOverload(value: Int): Int = value

    fun sideLayerEOverload(text: String): String = text
}

interface KotlinSideLayerF {
    fun sideLayerF0(value: Double): Double = value / 2.0

    fun sideLayerF1(value: Int): Boolean = value % 2 == 0

    fun sideLayerF2(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    fun sideLayerFOverload(value: Int): Int = value

    fun sideLayerFOverload(text: String): String = text
}

class KotlinFinalLayer : KotlinConcreteLayer11(), KotlinSideLayerB {
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

/*
 * All-Kotlin counterpart of `JavaFinalLayerNoDeclaredMembers`: same chain as `KotlinFinalLayer`, but
 * the leaf itself declares nothing, so every member reaching it is inherited.
 */
class KotlinFinalLayerNoDeclaredMembers : KotlinConcreteLayer11()

/*
 * All-Kotlin counterpart of `JavaFinalLayerNoParents`: no declared supertypes at all, carrying the
 * same 18 members the Java version declares. Isolates the fixed cost of reflect start-up from the
 * cost of walking a hierarchy.
 *
 * The six statics live in a companion object as `@JvmStatic` funs - the same nested-type asymmetry
 * `KotlinFinalLayer` has against `JavaFinalLayer`, since Kotlin has no class-level statics.
 */
class KotlinFinalLayerNoParents {
    fun concreteLayer110(value: Int): Int = value + 1

    fun concreteLayer111(text: String): String = text + "!"

    fun concreteLayer112(value: Long): Long = value * 2L

    fun concreteLayer113(value: Double): Double = value / 2.0

    fun concreteLayer114(value: Int): Boolean = value % 2 == 0

    fun concreteLayer115(text: CharSequence): CharSequence = text.length.toString() + ":" + text

    fun finalOwn0(value: Int): Int = value + 1

    fun finalOwn1(text: String): String = text + "!"

    fun finalOwn2(value: Long): Long = value * 2L

    fun finalOwn3(value: Double): Double = value / 2.0

    fun finalOwn4(value: Int): Boolean = value % 2 == 0

    fun finalOwn5(text: CharSequence): CharSequence = text.length.toString() + ":" + text

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
