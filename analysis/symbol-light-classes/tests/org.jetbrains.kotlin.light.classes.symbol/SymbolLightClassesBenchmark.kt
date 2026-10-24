/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.javaInterop.asFacadePsiClass
import org.jetbrains.kotlin.analysis.api.javaInterop.asPsiClass
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalSourceOutOfBlockModificationEvent
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.classSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.PsiClassRenderer
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.jupiter.api.Test
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.management.ObjectName

/**
 * A local benchmark of symbol light classes, not a regular test.
 *
 * Phases:
 * - `render_cold`: global source invalidation (FIR and light classes are cold), then all light classes are rendered
 * - `render_fresh`: only the light class caches are dropped (FIR is warm), then all light classes are rendered
 * - `render_warm`: all light classes are rendered again
 * - `resolve_fresh` / `resolve_warm`: the same, with a traversal that mimics the calls of the Java resolver instead of the rendering
 * - `lookup_warm`: light classes are only requested through the Analysis API
 * - `finder_warm`: light classes are only requested through [JavaElementFinder]
 * - `list_cold`: global source invalidation, then the classes of every package are listed through [JavaElementFinder] and
 *   asked for their containing class, as the Java resolver does for an on-demand import
 *
 * System properties: `slc.bench.phases` (comma-separated, default: all), `slc.bench.rounds` (default 10),
 * `slc.bench.warmup` (default 3), `slc.bench.label`, `prof.out.dir` (the directory of `slc-bench.tsv`).
 */
class SymbolLightClassesBenchmark : AbstractAnalysisApiExecutionTest(testDirPathString = "testData/benchmark") {
    override val configurator = LLSourceLikeTestConfigurator()

    @Test
    fun synthetic40(module: KtTestModule) {
        runBenchmark("synthetic40", module.ktFiles)
    }

    private enum class Phase(val repetitions: Int) {
        RENDER_COLD(1), RENDER_FRESH(1), RENDER_WARM(5), RESOLVE_FRESH(1), RESOLVE_WARM(20), LOOKUP_WARM(20), FINDER_WARM(20),
        LIST_COLD(1),
    }

    private var blackhole = 0L

    private fun runBenchmark(name: String, files: List<KtFile>) {
        val project = files.first().project
        val phases = System.getProperty("slc.bench.phases")?.split(',')?.map { Phase.valueOf(it.trim().uppercase()) } ?: Phase.entries
        val rounds = Integer.getInteger("slc.bench.rounds", 10)
        val warmup = Integer.getInteger("slc.bench.warmup", 3)
        val label = System.getProperty("slc.bench.label", "default")
        val output = System.getProperty("prof.out.dir")?.let { Paths.get(it, "slc-bench.tsv") }

        val classesByFile = files.associateWith { PsiTreeUtil.collectElementsOfType(it, KtClassOrObject::class.java).toList() }
        val fqNames = classesByFile.values.flatten().filter { it !is KtEnumEntry && !it.isLocal }.mapNotNull { it.fqName?.asString() }
        val packageNames = files.map { it.packageFqName.asString() }.distinct()

        val initial = lightClasses(classesByFile)
        initial.forEach { blackhole += PsiClassRenderer.renderClass(it).length }
        println(
            "SLC benchmark $name: ${initial.size} light classes, ${initial.sumOf { it.methods.size }} methods, " +
                    "${initial.sumOf { it.fields.size }} fields"
        )

        for (phase in phases) {
            val cpu = LongArray(rounds)
            val allocated = LongArray(rounds)
            for (round in 0 until warmup + rounds) {
                when (phase) {
                    Phase.RENDER_COLD, Phase.LIST_COLD -> runWriteAction { project.publishGlobalSourceOutOfBlockModificationEvent() }
                    Phase.RENDER_FRESH, Phase.RESOLVE_FRESH -> invalidateLightClassCaches(project)
                    else -> {}
                }

                val [cpuTime, allocatedBytes] = measure {
                    repeat(phase.repetitions) { runPhase(phase, classesByFile, fqNames, packageNames, project) }
                }

                if (round >= warmup) {
                    cpu[round - warmup] = cpuTime
                    allocated[round - warmup] = allocatedBytes
                }
            }

            val sortedCpu = cpu.sorted()
            val sortedAllocated = allocated.sorted()
            println(
                "SLC benchmark $name $phase x${phase.repetitions}: cpu median ${sortedCpu[rounds / 2] / 1e6} ms, " +
                        "min ${sortedCpu.first() / 1e6} ms, allocated median ${sortedAllocated[rounds / 2] / 1e6} MB"
            )

            if (output != null) {
                val lines = (0 until rounds).map { "$label\t$name\t${phase.name.lowercase()}\t$it\t${cpu[it]}\t${allocated[it]}\n" }
                Files.write(output, lines.joinToString("").toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            }
        }

        println("SLC benchmark $name: blackhole $blackhole")

        if (System.getProperty("slc.bench.histogram") == "true") {
            reportRetainedLightClasses(name, label, classesByFile, project)
        }
    }

    /**
     * Reports the live objects that a full rendering of fresh light classes adds over warm FIR, by class.
     */
    private fun reportRetainedLightClasses(
        name: String,
        label: String,
        classesByFile: Map<KtFile, List<KtClassOrObject>>,
        project: Project,
    ) {
        invalidateLightClassCaches(project)
        val before = classHistogram()
        val retained = lightClasses(classesByFile)
        retained.forEach { blackhole += PsiClassRenderer.renderClass(it).length }
        val after = classHistogram()

        val delta = after.mapNotNull { [className, counts] ->
            val old = before[className] ?: longArrayOf(0, 0)
            Triple(className, counts[0] - old[0], counts[1] - old[1]).takeIf { it.third > 0 }
        }.sortedByDescending { it.third }

        println("SLC benchmark $name retained by ${retained.size} light classes: ${delta.sumOf { it.third } / 1e6} MB")
        for ([className, instances, bytes] in delta.take(60)) {
            println("SLC benchmark retained ${bytes / 1000} KB $instances instances $className")
        }

        System.getProperty("prof.out.dir")?.let { directory ->
            val lines = delta.joinToString("") { [className, instances, bytes] -> "$label\t$className\t$instances\t$bytes\n" }
            Files.write(Paths.get(directory, "slc-retained.tsv"), lines.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        }

        blackhole += retained.size
    }

    /**
     * Live instances and bytes by class name, after a full GC.
     */
    private fun classHistogram(): Map<String, LongArray> {
        val text = ManagementFactory.getPlatformMBeanServer().invoke(
            ObjectName("com.sun.management:type=DiagnosticCommand"),
            "gcClassHistogram",
            arrayOf<Any>(arrayOf<String>()),
            arrayOf(Array<String>::class.java.name),
        ) as String

        return text.lineSequence().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 4 || !parts[0].endsWith(":")) return@mapNotNull null
            parts[3] to longArrayOf(parts[1].toLong(), parts[2].toLong())
        }.toMap()
    }

    private fun runPhase(
        phase: Phase,
        classesByFile: Map<KtFile, List<KtClassOrObject>>,
        fqNames: List<String>,
        packageNames: List<String>,
        project: Project,
    ) {
        when (phase) {
            Phase.RENDER_COLD, Phase.RENDER_FRESH, Phase.RENDER_WARM -> {
                for (lightClass in lightClasses(classesByFile)) {
                    blackhole += PsiClassRenderer.renderClass(lightClass).length
                }
            }

            Phase.RESOLVE_FRESH, Phase.RESOLVE_WARM -> {
                for (lightClass in lightClasses(classesByFile)) {
                    blackhole += resolveLike(lightClass)
                }
            }

            Phase.LOOKUP_WARM -> {
                blackhole += lightClasses(classesByFile).size
            }

            Phase.FINDER_WARM -> {
                val finder = JavaElementFinder.getInstance(project)
                val scope = GlobalSearchScope.allScope(project)
                for (fqName in fqNames) {
                    blackhole += finder.findClasses(fqName, scope).size
                }
            }

            Phase.LIST_COLD -> {
                val finder = JavaElementFinder.getInstance(project)
                val scope = GlobalSearchScope.allScope(project)
                for (packageName in packageNames) {
                    val psiPackage = finder.findPackage(packageName) ?: continue
                    for (psiClass in finder.getClasses(psiPackage, scope)) {
                        blackhole += psiClass.qualifiedName.hashCode() + psiClass.containingClass.hashCode()
                    }
                }
            }
        }
    }

    private fun lightClasses(classesByFile: Map<KtFile, List<KtClassOrObject>>): List<PsiClass> {
        val result = mutableListOf<PsiClass>()
        for ([file, classes] in classesByFile) {
            analyze(file) {
                file.symbol.asFacadePsiClass()?.let(result::add)
                for (declaration in classes) {
                    val lightClass = when (declaration) {
                        is KtEnumEntry -> declaration.symbol.initializer?.asPsiClass()
                        else -> declaration.classSymbol?.asPsiClass()
                    }

                    lightClass?.let(result::add)
                }
            }
        }

        return result
    }

    /**
     * Calls what the Java resolver and highlighting typically call for a referenced class and its members.
     */
    private fun resolveLike(psiClass: PsiClass): Long {
        var hash = 0L
        hash += psiClass.name.hashCode() + psiClass.qualifiedName.hashCode()
        if (psiClass.isInterface) hash++
        if (psiClass.isEnum) hash++
        if (psiClass.isRecord) hash++
        if (psiClass.isAnnotationType) hash++
        if (psiClass.hasTypeParameters()) hash++
        hash += psiClass.typeParameters.size
        hash += psiClass.parent.hashCode() + psiClass.containingClass.hashCode() + psiClass.containingFile.hashCode()
        if (psiClass.isValid) hash++
        if (psiClass.isDeprecated) hash++
        for (modifier in MODIFIERS) {
            if (psiClass.hasModifierProperty(modifier)) hash++
        }

        hash += psiClass.superClass.hashCode()
        hash += psiClass.interfaces.size + psiClass.superTypes.size + psiClass.extendsListTypes.size + psiClass.implementsListTypes.size

        for (method in psiClass.methods) {
            hash += method.name.hashCode()
            if (method.isConstructor) hash++
            if (method.isVarArgs) hash++
            if (method.hasTypeParameters()) hash++
            hash += method.typeParameters.size
            hash += method.returnType.hashCode()
            hash += method.parameterList.parametersCount
            for (parameter in method.parameterList.parameters) {
                hash += parameter.type.hashCode() + parameter.name.hashCode()
                if (parameter.isVarArgs) hash++
            }

            for (modifier in MODIFIERS) {
                if (method.hasModifierProperty(modifier)) hash++
            }

            if (method.isDeprecated) hash++
            if (method.modifierList.findAnnotation(NOT_NULL) != null) hash++
            if (method.modifierList.findAnnotation(NULLABLE) != null) hash++
            hash += method.containingClass.hashCode()
            if (method.isValid) hash++
            hash += method.getSignature(PsiSubstitutor.EMPTY).hashCode()
        }

        for (field in psiClass.fields) {
            hash += field.name.hashCode() + field.type.hashCode()
            for (modifier in MODIFIERS) {
                if (field.hasModifierProperty(modifier)) hash++
            }

            if (field.isDeprecated) hash++
            if (field.hasInitializer()) hash++
            if (field.isValid) hash++
        }

        for (method in psiClass.methods) {
            hash += psiClass.findMethodsByName(method.name, false).size
        }

        return hash
    }

    private fun invalidateLightClassCaches(project: Project) {
        val support = KotlinAsJavaSupport.getInstance(project)
        for (fieldName in listOf("moduleBasedLightClassCache", "moduleBasedPsiLightClassCache", "calculatedContextModuleCache")) {
            val field = support.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
            val cache = field.get(support)
            cache.javaClass.getMethod("invalidateAll").invoke(cache)
        }
    }

    private inline fun measure(block: () -> Unit): LongArray {
        val threadId = Thread.currentThread().id
        val cpuBefore = threadBean.currentThreadCpuTime
        val allocatedBefore = threadBean.getThreadAllocatedBytes(threadId)
        block()
        return longArrayOf(threadBean.currentThreadCpuTime - cpuBefore, threadBean.getThreadAllocatedBytes(threadId) - allocatedBefore)
    }

    private companion object {
        val threadBean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean

        val MODIFIERS = listOf(
            PsiModifier.PUBLIC, PsiModifier.PROTECTED, PsiModifier.PRIVATE, PsiModifier.PACKAGE_LOCAL,
            PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.ABSTRACT,
        )

        const val NOT_NULL = "org.jetbrains.annotations.NotNull"
        const val NULLABLE = "org.jetbrains.annotations.Nullable"
    }
}
