/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.ClassReference

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