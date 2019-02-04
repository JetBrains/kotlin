/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.junit.Assert

abstract class AbstractFileRankingTest : LowLevelDebuggerTestBase() {
    override fun doTest(
        options: Set<String>,
        mainThread: ThreadReference,
        factory: OriginCollectingClassBuilderFactory,
        classFileFactory: ClassFileFactory,
        state: GenerationState
    ) {
        val allKtFiles = classFileFactory.inputFiles.distinct()
        fun getKtFiles(name: String) = allKtFiles.filter { it.name == name }

        val doNotCheckClassFqName = "DO_NOT_CHECK_CLASS_FQNAME" in options
        val strictMode = "DISABLE_STRICT_MODE" !in options

        val expectedRanks: Map<Pair<KtFile, Int>, Int> = allKtFiles.asSequence().flatMap { ktFile ->
            ktFile.text.lines()
                .asSequence()
                .withIndex()
                .map {
                    val matchResult = "^.*// (R: (-?\\d+)( L: (\\d+))?)\\s*$".toRegex().matchEntire(it.value) ?: return@map null

                    val rank = matchResult.groupValues[2].toInt()
                    val line = matchResult.groupValues.getOrNull(4)?.takeIf { !it.isEmpty() }?.toInt()

                    if (line != null && line != it.index + 1) {
                        throw IllegalArgumentException("Bad line in directive at ${ktFile.name}:${it.index + 1}\n${it.value}")
                    }

                    (ktFile to it.index + 1) to rank
                }
                .filterNotNull()
        }.toMap()

        val calculator = object : FileRankingCalculator(checkClassFqName = !doNotCheckClassFqName) {
            override fun analyze(element: KtElement) = state.bindingContext
        }

        val problems = mutableListOf<String>()

        val classNameToKtFile = factory.origins.asSequence()
            .filter { it.key is ClassNode }
            .map {
                val ktFile = (it.value.element?.containingFile as? KtFile) ?: return@map null
                val name = (it.key as ClassNode).name.replace('/', '.')

                name to ktFile
            }
            .filterNotNull()
            .toMap()

        val skipClasses = skipLoadingClasses(options)
        for (outputFile in classFileFactory.getClassFiles()) {
            val className = outputFile.internalName.replace('/', '.')
            if (className in skipClasses) {
                continue
            }

            val expectedFile = classNameToKtFile[className] ?: throw IllegalStateException("Can't find source for $className")

            val jdiClass = mainThread.virtualMachine().classesByName(className).singleOrNull()
                ?: error("Class '$className' was not found in the debuggee process class loader")

            val locations = jdiClass.allLineLocations()
            assert(locations.isNotEmpty()) { "There are no locations for class $className" }

            val allFilesWithSameName = getKtFiles(expectedFile.name)

            for (location in locations) {
                if (location.method().isBridge || location.method().isSynthetic) continue

                val fileWithRankings: Map<KtFile, Int> = calculator.rankFiles(allFilesWithSameName, location)

                for ((ktFile, rank) in fileWithRankings) {
                    val expectedRank = expectedRanks[ktFile to (location.lineNumber())]
                    if (expectedRank != null) {
                        Assert.assertEquals("Invalid expected rank at $location", expectedRank, rank)
                    }
                }

                val fileWithMaxScore = fileWithRankings.maxBy { it.value }!!
                val actualFile = fileWithMaxScore.key

                if (strictMode) {
                    require(fileWithMaxScore.value >= 0) { "Max score is negative at $location" }

                    // Allow only one element with max ranking
                    require(fileWithRankings.filter { it.value == fileWithMaxScore.value }.count() == 1) {
                        "Score is the same for several files at $location"
                    }
                }

                if (actualFile != expectedFile) {
                    problems += "Location ${location.sourceName()}:${location.lineNumber() - 1} is associated with a wrong KtFile:\n" +
                            "    - expected: ${expectedFile.virtualFilePath}\n" +
                            "    - actual: ${actualFile.virtualFilePath}"
                }
            }
        }

        if (problems.isNotEmpty()) {
            throw AssertionError(buildString {
                appendln("There were association errors:").appendln()
                problems.joinTo(this, "\n\n")
            })
        }
    }

    override fun skipLoadingClasses(options: Set<String>): Set<String> {
        val skipClasses = options.mapTo(mutableSetOf()) { it.substringAfter("DO_NOT_LOAD:", "").trim() }
        skipClasses.remove("")
        return skipClasses
    }
}