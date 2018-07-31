package org.jetbrains.kotlin.idea.debugger

import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.org.objectweb.asm.tree.ClassNode

abstract class AbstractFileRankingTest : LowLevelDebuggerTestBase() {
    override fun doTest(
        options: Set<String>,
        mainThread: ThreadReference,
        factory: OriginCollectingClassBuilderFactory,
        state: GenerationState
    ) {
        val allKtFiles = factory.origins.mapNotNull { it.value.element?.containingFile as? KtFile }.distinct()
        fun getKtFiles(name: String) = allKtFiles.filter { it.name == name }

        val doNotCheckClassFqName = "DO_NOT_CHECK_CLASS_FQNAME" in options

        val calculator = object : FileRankingCalculator(checkClassFqName = !doNotCheckClassFqName, strictMode = true) {
            override fun analyze(element: KtElement) = state.bindingContext
        }

        val problems = mutableListOf<String>()

        val skipClasses = skipLoadingClasses(options)
        for ((node, origin) in factory.origins) {
            val classNode = node as? ClassNode ?: continue
            val expectedFile = origin.element?.containingFile as? KtFile ?: continue
            val className = classNode.name.replace('/', '.')

            if (className in skipClasses) {
                continue
            }

            val jdiClass = mainThread.virtualMachine().classesByName(className).singleOrNull()
                ?: error("Class '$className' was not found in the debuggee process class loader")

            val locations = jdiClass.allLineLocations()
            assert(locations.isNotEmpty()) { "There are no locations for class $className" }

            val allFilesWithSameName = getKtFiles(expectedFile.name)

            for (location in locations) {
                if (location.method().isBridge || location.method().isSynthetic) continue

                val actualFile = calculator.findMostAppropriateSource(allFilesWithSameName, location)
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