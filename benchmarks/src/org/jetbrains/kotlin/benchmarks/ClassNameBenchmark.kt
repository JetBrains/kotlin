/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.ClassReference
import kotlin.random.Random

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class ClassNameBenchmark {
    val classes = listOf(
        java.lang.annotation.Annotation::class.java,
        java.lang.Boolean::class.java,
        java.lang.Byte::class.java,
        java.lang.Character::class.java,
        java.lang.CharSequence::class.java,
        java.lang.Cloneable::class.java,
        java.lang.Comparable::class.java,
        java.lang.Double::class.java,
        java.lang.Enum::class.java,
        java.lang.Float::class.java,
        java.lang.Integer::class.java,
        java.lang.Iterable::class.java,
        java.lang.Long::class.java,
        java.lang.Number::class.java,
        java.lang.Object::class.java,
        java.lang.Short::class.java,
        java.lang.String::class.java,
        java.lang.Throwable::class.java,
        java.util.Collection::class.java,
        java.util.Iterator::class.java,
        java.util.ListIterator::class.java,
        java.util.List::class.java,
        java.util.Map.Entry::class.java,
        java.util.Map::class.java,
        java.util.Set::class.java,
        kotlin.jvm.functions.Function0::class.java,
        kotlin.jvm.functions.Function1::class.java,
        kotlin.jvm.functions.Function2::class.java,
        kotlin.jvm.functions.Function3::class.java,
        kotlin.jvm.functions.Function4::class.java,
        kotlin.jvm.functions.Function5::class.java,
        kotlin.jvm.functions.Function6::class.java,
        kotlin.jvm.functions.Function7::class.java,
        kotlin.jvm.functions.Function8::class.java,
        kotlin.jvm.functions.Function9::class.java,
        kotlin.jvm.functions.Function10::class.java,
        kotlin.jvm.functions.Function11::class.java,
        kotlin.jvm.functions.Function12::class.java,
        kotlin.jvm.functions.Function13::class.java,
        kotlin.jvm.functions.Function14::class.java,
        kotlin.jvm.functions.Function15::class.java,
        kotlin.jvm.functions.Function16::class.java,
        kotlin.jvm.functions.Function17::class.java,
        kotlin.jvm.functions.Function18::class.java,
        kotlin.jvm.functions.Function19::class.java,
        kotlin.jvm.functions.Function20::class.java,
        kotlin.jvm.functions.Function21::class.java,
        kotlin.jvm.functions.Function22::class.java,
        Boolean.Companion::class.java,
        Byte.Companion::class.java,
        Char.Companion::class.java,
        Double.Companion::class.java,
        Enum.Companion::class.java,
        Float.Companion::class.java,
        Int.Companion::class.java,
        Long.Companion::class.java,
        Short.Companion::class.java,
        String.Companion::class.java,
    )

    @Benchmark
    fun classSimpleName(bh: Blackhole) {
        for (klass in classes) {
            bh.consume(ClassReference.getClassSimpleName(klass))
        }
    }

    @Benchmark
    fun classQualifiedName(bh: Blackhole) {
        for (klass in classes) {
            bh.consume(ClassReference.getClassQualifiedName(klass))
        }
    }
}

// Match the number of system classes
class UDT01
class UDT02
class UDT03
class UDT04
class UDT05
class UDT06
class UDT07
class UDT08
class UDT09
class UDT10
class UDT11
class UDT12
class UDT13
class UDT14
class UDT15
class UDT16
class UDT17
class UDT18
class UDT19
class UDT20
class UDT21
class UDT22
class UDT23
class UDT24
class UDT25
class UDT26
class UDT27
class UDT28
class UDT29
class UDT30
class UDT31
class UDT32
class UDT33
class UDT34
class UDT35
class UDT36
class UDT37
class UDT38
class UDT39
class UDT40
class UDT41
class UDT42
class UDT43
class UDT44
class UDT45
class UDT46
class UDT47
class UDT48
class UDT49
class UDT50
class UDT51
class UDT52
class UDT53
class UDT54
class UDT55
class UDT56
class UDT57


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class ClassNameBenchmarkWithUserDefinedTypes {
    @Param("1.0", "0.75", "0.25", "0.05")
    var systemTypesRatio: Double = 0.0

    val systemTypes = listOf(
        java.lang.annotation.Annotation::class.java,
        java.lang.Boolean::class.java,
        java.lang.Byte::class.java,
        java.lang.Character::class.java,
        java.lang.CharSequence::class.java,
        java.lang.Cloneable::class.java,
        java.lang.Comparable::class.java,
        java.lang.Double::class.java,
        java.lang.Enum::class.java,
        java.lang.Float::class.java,
        java.lang.Integer::class.java,
        java.lang.Iterable::class.java,
        java.lang.Long::class.java,
        java.lang.Number::class.java,
        java.lang.Object::class.java,
        java.lang.Short::class.java,
        java.lang.String::class.java,
        java.lang.Throwable::class.java,
        java.util.Collection::class.java,
        java.util.Iterator::class.java,
        java.util.ListIterator::class.java,
        java.util.List::class.java,
        java.util.Map.Entry::class.java,
        java.util.Map::class.java,
        java.util.Set::class.java,
        kotlin.jvm.functions.Function0::class.java,
        kotlin.jvm.functions.Function1::class.java,
        kotlin.jvm.functions.Function2::class.java,
        kotlin.jvm.functions.Function3::class.java,
        kotlin.jvm.functions.Function4::class.java,
        kotlin.jvm.functions.Function5::class.java,
        kotlin.jvm.functions.Function6::class.java,
        kotlin.jvm.functions.Function7::class.java,
        kotlin.jvm.functions.Function8::class.java,
        kotlin.jvm.functions.Function9::class.java,
        kotlin.jvm.functions.Function10::class.java,
        kotlin.jvm.functions.Function11::class.java,
        kotlin.jvm.functions.Function12::class.java,
        kotlin.jvm.functions.Function13::class.java,
        kotlin.jvm.functions.Function14::class.java,
        kotlin.jvm.functions.Function15::class.java,
        kotlin.jvm.functions.Function16::class.java,
        kotlin.jvm.functions.Function17::class.java,
        kotlin.jvm.functions.Function18::class.java,
        kotlin.jvm.functions.Function19::class.java,
        kotlin.jvm.functions.Function20::class.java,
        kotlin.jvm.functions.Function21::class.java,
        kotlin.jvm.functions.Function22::class.java,
        Boolean.Companion::class.java,
        Byte.Companion::class.java,
        Char.Companion::class.java,
        Double.Companion::class.java,
        Enum.Companion::class.java,
        Float.Companion::class.java,
        Int.Companion::class.java,
        Long.Companion::class.java,
        Short.Companion::class.java,
        String.Companion::class.java,
    )

    val userDefinedTypes = listOf(
        UDT01::class.java,
        UDT02::class.java,
        UDT03::class.java,
        UDT04::class.java,
        UDT05::class.java,
        UDT06::class.java,
        UDT07::class.java,
        UDT08::class.java,
        UDT09::class.java,
        UDT10::class.java,
        UDT11::class.java,
        UDT12::class.java,
        UDT13::class.java,
        UDT14::class.java,
        UDT15::class.java,
        UDT16::class.java,
        UDT17::class.java,
        UDT18::class.java,
        UDT19::class.java,
        UDT20::class.java,
        UDT21::class.java,
        UDT22::class.java,
        UDT23::class.java,
        UDT24::class.java,
        UDT25::class.java,
        UDT26::class.java,
        UDT27::class.java,
        UDT28::class.java,
        UDT29::class.java,
        UDT30::class.java,
        UDT31::class.java,
        UDT32::class.java,
        UDT33::class.java,
        UDT34::class.java,
        UDT35::class.java,
        UDT36::class.java,
        UDT37::class.java,
        UDT38::class.java,
        UDT39::class.java,
        UDT40::class.java,
        UDT41::class.java,
        UDT42::class.java,
        UDT43::class.java,
        UDT44::class.java,
        UDT45::class.java,
        UDT46::class.java,
        UDT47::class.java,
        UDT48::class.java,
        UDT49::class.java,
        UDT50::class.java,
        UDT51::class.java,
        UDT52::class.java,
        UDT53::class.java,
        UDT54::class.java,
        UDT55::class.java,
        UDT56::class.java,
        UDT57::class.java
    )

    var classes = listOf<Class<out Any>>()

    @Setup
    fun setupClassesList() {
        classes =
            (systemTypes.take((systemTypes.size * systemTypesRatio).toInt()) +
                    userDefinedTypes.take((userDefinedTypes.size * (1 - systemTypesRatio)).toInt()))
                .shuffled(Random(42))
    }

    @Benchmark
    fun classSimpleName(bh: Blackhole) {
        for (klass in classes) {
            bh.consume(ClassReference.getClassSimpleName(klass))
        }
    }

    @Benchmark
    fun classQualifiedName(bh: Blackhole) {
        for (klass in classes) {
            bh.consume(ClassReference.getClassQualifiedName(klass))
        }
    }
}
