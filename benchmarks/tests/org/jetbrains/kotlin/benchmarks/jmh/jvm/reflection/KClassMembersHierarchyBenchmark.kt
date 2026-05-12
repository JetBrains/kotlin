/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.data.*
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.openjdk.jmh.annotations.Level
import java.lang.reflect.Method
import java.net.URLClassLoader

/**
 * Gathers `KClass.members` for hierarchies of varying shape and language mix.
 *
 * kotlin-reflect is used the same way the compiler tests use it (see `ReflectionIntegrationTest`): the
 * locally built stdlib and kotlin-reflect jars arrive as system properties and are loaded into a
 * dedicated class loader by [StandardLibrariesPathProviderForKotlinProject], which also picks the K1 or
 * the new implementation. Nothing of kotlin-reflect is on this benchmark's own classpath, so the
 * measured code cannot accidentally link against a published build of it.
 *
 * Because the measured `members` call happens in that class loader, it is reached through [MembersDumper]
 * reflectively - the cost of the single `Method.invoke` is negligible next to gathering the members.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
open class KClassMembersHierarchyBenchmark {
    @Param("new", "k1")
    private var reflectImplementation: String = ""

    private lateinit var isolatedClassLoader: ClassLoader
    private lateinit var dumper: Any
    private lateinit var dumpMembers: Method
    private lateinit var describeMember: Method

    @Setup(Level.Trial)
    fun setUp() {
        val provider = StandardLibrariesPathProviderForKotlinProject
        // Both getters assert the implementation they promise is the one actually in effect, so a run
        // can never silently measure the wrong one.
        val reflectClassLoader = when (reflectImplementation) {
            "new" -> provider.getRuntimeAndReflectJarClassLoader()
            "k1" -> provider.getRuntimeAndK1ReflectJarClassLoader()
            else -> error("Unknown reflect implementation: $reflectImplementation")
        }

        // The dumper and the hierarchies have to be loaded below the jars above, so that they link
        // against that kotlin-reflect rather than against whatever this benchmark was compiled with.
        val ownCode = listOf(MembersDumper::class.java, JavaFinalLayer::class.java)
            .map { it.protectionDomain.codeSource.location }
            .distinct()
            .toTypedArray()
        isolatedClassLoader = URLClassLoader(ownCode, reflectClassLoader)

        val dumperClass = isolatedClassLoader.loadClass(MembersDumper::class.java.name)
        dumper = dumperClass.getDeclaredConstructor().newInstance()
        dumpMembers = dumperClass.getMethod("dumpMembers", ClassLoader::class.java, String::class.java)
        describeMember = dumperClass.getMethod(
            "describeMember", ClassLoader::class.java, String::class.java, String::class.java
        )
    }

    private fun members(target: Class<*>): String =
        dumpMembers.invoke(dumper, isolatedClassLoader, target.name) as String

    private fun describe(target: Class<*>, memberName: String): String =
        describeMember.invoke(dumper, isolatedClassLoader, target.name, memberName) as String

    @Benchmark
    open fun javaHierarchy(): String {
        return members(JavaFinalLayer::class.java)
    }

    @Benchmark
    open fun javaHierarchyNoDeclaredMembersLeaf(): String {
        return members(JavaFinalLayerNoDeclaredMembers::class.java)
    }

    @Benchmark
    open fun javaLeafWithoutActualParents(): String {
        return members(JavaFinalLayerNoParents::class.java)
    }

    @Benchmark
    open fun kotlinHierarchy(): String {
        return members(KotlinFinalLayer::class.java)
    }

    @Benchmark
    open fun kotlinHierarchyNoDeclaredMembersLeaf(): String {
        return members(KotlinFinalLayerNoDeclaredMembers::class.java)
    }

    @Benchmark
    open fun kotlinLeafWithoutActualParents(): String {
        return members(KotlinFinalLayerNoParents::class.java)
    }

    @Benchmark
    open fun mixedHierarchyJavaLeaf(): String {
        return members(MixedFinalLayerJava::class.java)
    }

    @Benchmark
    open fun mixedHierarchyKotlinLeaf(): String {
        return members(MixedFinalLayerKotlin::class.java)
    }
}
