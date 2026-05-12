/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.reflection

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import org.openjdk.jmh.annotations.Level
import kotlin.reflect.KClass

@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
open class KClassMembersHierarchyBenchmark {
    private val targetClass1: KClass<out JavaFinalLayer> = JavaFinalLayer::class
    private val targetClass2: KClass<out JavaFinalLayerNoDeclaredMembers> = JavaFinalLayerNoDeclaredMembers::class
    private val targetClass3: KClass<out JavaFinalLayerNoParents> = JavaFinalLayerNoParents::class

    @TearDown(Level.Trial)
    fun after() {
        println("DEBUG Own (finalOwn1): " + targetClass1.members.find { it.name == "finalOwn1" }!!::class)
        println("DEBUG Static (finalOwnStatic0):" + targetClass1.members.find { it.name == "finalOwnStatic0" }!!::class)
        println("DEBUG Base (abstractBase0): " + targetClass1.members.find { it.name == "abstractBase0" }!!::class)
        println("DEBUG Gen (equals):" + targetClass1.members.find { it.name == "equals" }!!::class)
    }

    @Benchmark
    open fun hierarchy(): String {
        return targetClass1.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun noDeclaredMembers(): String {
        return targetClass2.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun noParents(): String {
        return targetClass3.members.joinToString { it.toString() }
    }

}
