/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.computeDumpExtension
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.groupWithTestFiles
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.NO_SIGNATURE_DUMP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_KT_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.manglerProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.same
import java.io.File

private const val CHECK_MARKER = "// CHECK"

/**
 * Uses [IrElement.dumpKotlinLike] to compare the human-readable representation of an IR tree with
 * an expected output in a `*.kt.txt` file located next to the test file.
 *
 * This handler can be enabled by specifying the [DUMP_KT_IR] test directive,
 * or disabled with the [SKIP_KT_DUMP] directive.
 *
 * ## Signatures
 * In addition to dumping the Kotlin-like representation of an IR tree, this handler prints a mangled name and
 * an [IdSignature] before each declaration. This behavior can be disabled with the [NO_SIGNATURE_DUMP] test directive.
 *
 * Since mangled names and signatures may be different depending on the backend, in order to reduce the number
 * of expectation files, this handler uses `// CHECK` blocks to compare the dump with an expectation in a smarter way than
 * just character by character.
 *
 * For example, the expectation file may look like this:
 *
 * ```kotlin
 * // CHECK JVM_IR:
 * //   Mangled name: #test(){}kotlin.Int
 * //   Public signature: /test|4216975235718029399[0]
 * // CHECK JS_IR NATIVE:
 * //   Mangled name: #test(){}
 * //   Public signature: /test|6620506149988718649[0]
 * fun test(): Int {
 *   return 42
 * }
 * ```
 *
 * Here, on the JVM backend the mangled names include the return value, while on the JS and Native backends
 * this is not the case, so we use two `// CHECK` blocks to reflect that fact.
 *
 * When running a test against some backend, we do the following.
 *
 * 1. In order to compare this expectation with the actual [IrElement.dumpKotlinLike] output, we first parse
 * all the `// CHECK` block groups in the expectation file in such a way that the number of such groups
 * matches the number of declarations.
 *
 * 2. When running [IrElement.dumpKotlinLike], for each declaration that we want to print the mangled name and
 * the signature for, we take the next parsed group, and iterate over `// CHECK` blocks in that group.
 *
 * 3. For each `// CHECK` block the following logic is applied:
 *    - If the current `// CHECK` block includes the backend that we're running this test against, or doesn't include
 *      any backends, we print the actual mangled name and signature for this declaration.
 *    - Otherwise, we print the `// CHECK` block as is.
 */
class IrPrettyKotlinDumpHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    companion object {
        const val DUMP_EXTENSION = "kt.txt"
    }

    private val dumper = MultiModuleInfoDumper("// MODULE: %s")

    private val expectedFile: File by lazy(LazyThreadSafetyMode.NONE) {
        val moduleStructure = testServices.moduleStructure
        val extension = computeDumpExtension(moduleStructure.modules.first(), DUMP_EXTENSION)
        moduleStructure.originalTestDataFiles.first().withExtension(extension)
    }

    /**
     * Iterates over parsed `// CHECK` block groups in [expectedFile].
     *
     * Each element is a list of `// CHECK` blocks for a single declaration.
     */
    private val checkBlockGroupIterator: Iterator<CheckBlockGroup> by lazy(LazyThreadSafetyMode.NONE) {
        val expectations = expectedFile.takeIf { it.exists() }?.let { parseAllCheckBlocks(it.readText()) }.orEmpty()
        expectations.iterator()
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_KT_IR !in module.directives || SKIP_KT_DUMP in module.directives) return

        if (NO_SIGNATURE_DUMP in module.directives) {
            runDumper(module, info, KotlinLikeDumpOptions.NOOP_CALLBACK, KotlinLikeDumpOptions.NOOP_CALLBACK)
            return
        }

        val targetBackend = module.targetBackend!!

        val (descriptorMangler, irMangler, firMangler) = testServices.manglerProvider.getManglersForModule(module)

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        fun Printer.printSignatureAndMangledName(element: IrElement) {
            if (element is IrFunctionExpression) {
                // Function expressions are printed on the same line by default. We don't want that.
                println()
                pushIndent()
                printIndent()
            }

            // Manglers don't support local variables and local delegated properties.
            if (element !is IrDeclaration || element is IrVariable || element is IrLocalDelegatedProperty)
                return

            val symbol = element.symbol

            val computedMangledNames = mutableListOf<ComputedMangledName>()

            irMangler.addMangledNameTo(computedMangledNames, element)
            descriptorMangler.addMangledNameTo(computedMangledNames, symbol.descriptor)
            ((element as? IrMetadataSourceOwner)?.metadata as? FirMetadataSource)?.fir?.let {
                firMangler?.addMangledNameTo(
                    computedMangledNames,
                    it
                )
            }

            fun printAllInfo() {
                printMangledNames(computedMangledNames)

                symbol.signature?.let {
                    println("//   Public signature: ${it.render()}")
                }
                symbol.privateSignature?.let {
                    println("//   Private signature: ${it.render()}")
                }
            }

            var printedActualMangledNameAndSignature = false
            if (checkBlockGroupIterator.hasNext()) {
                val checkBlockGroupForDeclaration = checkBlockGroupIterator.next()
                for (checkBlock in checkBlockGroupForDeclaration) {
                    printlnCheckMarker(checkBlock.backends)
                    if (checkBlock.backends.isEmpty() || targetBackend in checkBlock.backends) {
                        // If this // CHECK block corresponds to the current target backend, print the actual values
                        printAllInfo()
                        printedActualMangledNameAndSignature = true
                    } else {
                        // Otherwise, print the // CHECK block from the expectation file as is
                        checkBlock.expectations.forEach(this::println)
                    }
                }
                if (!printedActualMangledNameAndSignature) {
                    printlnCheckMarker(listOf(targetBackend))
                    printAllInfo()
                }
            } else {
                printlnCheckMarker(emptyList())
                printAllInfo()
            }
        }

        fun Printer.afterEachElement(element: IrElement) {
            if (element is IrFunctionExpression) {
                popIndent()
            }
        }

        runDumper(module, info, Printer::printSignatureAndMangledName, Printer::afterEachElement)
    }

    private fun runDumper(
        module: TestModule,
        info: IrBackendInput,
        beforeEachElement: KotlinLikeDumpCallback,
        afterEachElement: KotlinLikeDumpCallback
    ) {
        val irFiles = info.irModuleFragment.files
        val builder = dumper.builderForModule(module)
        val filteredIrFiles = irFiles.groupWithTestFiles(module).filterNot {
            it.first?.directives?.contains(EXTERNAL_FILE) == true
        }.map { it.second }
        val printFileName = filteredIrFiles.size > 1 || testServices.moduleStructure.modules.size > 1
        for (irFile in filteredIrFiles) {
            val dump = irFile.dumpKotlinLike(
                KotlinLikeDumpOptions(
                    printFileName = printFileName,
                    printFilePath = false,
                    printFakeOverridesStrategy = FakeOverridesStrategy.NONE,
                    beforeEachElement = beforeEachElement,
                    afterEachElement = afterEachElement,
                )
            )
            builder.append(dump)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val dump = dumper.generateResultingDump()
        assertions.assertEqualsToFile(expectedFile, dump)
    }
}

private data class ComputedMangledName(
    val manglerName: String,
    val compatibleMode: Boolean,
    val value: String,
)

/**
 * For each pair in the list, pads its first element with a number of spaces and concatenates it with the second element in such a way
 * that the second elements of each pair are vertically aligned in concatenated strings.
 *
 * In other words, transforms this list:
 *
 * ```kotlin
 * listOf(
 *   Pair("Capacity: ", "normal"),
 *   Pair("Usability: ", "very good"),
 *   Pair("Magic: ", "minimal"),
 * )
 * ```
 *
 * to this:
 *
 * ```kotlin
 * listOf(
 *   "Capacity:  normal",
 *   "Usability: very good",
 *   "Magic:     minimal",
 * )
 * ```
 */
private fun Iterable<Pair<String, String>>.aligned(): List<String> {
    val maxPrefixLength = maxOf { it.first.length }
    return map { (prefix, suffix) -> prefix.padEnd(maxPrefixLength) + suffix }
}

private inline fun Iterable<ComputedMangledName>.printAligned(printer: Printer, prefix: (ComputedMangledName) -> String) {
    map { prefix(it) to it.value }.aligned().forEach(printer::println)
}

private fun Printer.printMangledNames(computedMangledNames: List<ComputedMangledName>) {
    val distinctNames = computedMangledNames.distinctBy { it.value }

    // If mangled names computed from all three representations (descriptors, IR, FIR) and modes match,
    // print just one mangled name.
    distinctNames.singleOrNull()?.let {
        println("//   Mangled name: ${it.value}")
        return
    }

    if (distinctNames.same { it.compatibleMode }) {
        // If the mangled names differ only by the mangler used (but not the compatibility mode), print
        // only mangled names from each mangler.
        computedMangledNames
            .distinctBy { it.manglerName }
            .printAligned(this) { "//   Mangled name computed from ${it.manglerName}: " }
    } else if (distinctNames.same { it.manglerName }) {
        // If the mangled names differ only by the compatibility mode used (but not the mangler), print
        // only mangled names for each compatibility mode.
        computedMangledNames
            .distinctBy { it.compatibleMode }
            .printAligned(this) { "//   Mangled name (compatible mode: ${it.compatibleMode}): " }
    } else {
        // Otherwise, print the whole matrix.
        computedMangledNames
            .printAligned(this) { "//   Mangled name computed from ${it.manglerName} (compatible mode: ${it.compatibleMode}): " }
    }
}

private fun Printer.printlnCheckMarker(backends: List<TargetBackend>) {
    print(CHECK_MARKER)
    for (backend in backends) {
        printWithNoIndent(" ")
        printWithNoIndent(backend.name)
    }
    printlnWithNoIndent(":")
}

private fun <Declaration, Mangler : KotlinMangler<Declaration>> Mangler.addMangledNameTo(
    collector: MutableList<ComputedMangledName>,
    declaration: Declaration
) {
    listOf(false, true).mapTo(collector) { compatibleMode ->
        val mangledName = try {
            declaration.mangleString(compatibleMode)
        } catch (e: Throwable) {
            // Kotlin-like IR renderer suppresses exceptions thrown during rendering, which leads to missing renders that are hard to debug.
            // Because this routine is executed during rendering, we print the exception description instead of a proper mangled name.
            val message = e.toString()
            buildString {
                append("could not compute mangled name: ")
                when (val newlineIndex = message.indexOf('\n')) {
                    -1 -> append(message)
                    else -> {
                        append(message.substring(0, newlineIndex))
                        append("... (truncated)")
                    }
                }
            }
        }
        ComputedMangledName(manglerName, compatibleMode, mangledName)
    }
}

/**
 * Represents a single `// CHECK` block.
 *
 * @property backends The backends this `// CHECK` block is for.
 * @property expectations The list of expectation lines in this `// CHECK` block.
 */
private data class CheckBlock(val backends: List<TargetBackend>, val expectations: List<String>)

/**
 * A list of `// CHECK` blocks for a single declaration.
 */
private typealias CheckBlockGroup = List<CheckBlock>

private fun parseAllCheckBlocks(input: String): List<CheckBlockGroup> {
    val result = mutableListOf<CheckBlockGroup>()
    val lineIterator = input.lines().iterator()

    var currentCheckBlockGroup = mutableListOf<CheckBlock>() // CHECK blocks for a single declaration

    var line = if (lineIterator.hasNext()) lineIterator.next() else return emptyList()

    while (true) {
        val trimmed = line.trim()
        if (trimmed.startsWith(CHECK_MARKER)) {
            val (nextCheckMarker, checkBlock) = parseSingleCheckBlock(trimmed, lineIterator)
            currentCheckBlockGroup.add(checkBlock)
            if (nextCheckMarker != null) {
                line = nextCheckMarker
                continue
            }

            result.add(currentCheckBlockGroup)
        }
        if (lineIterator.hasNext()) {
            line = lineIterator.next()
            currentCheckBlockGroup = mutableListOf()
        } else {
            break
        }
    }
    return result
}

private val whitespaceRegex = "\\s+".toRegex()

/**
 * Parses a single check block.
 *
 * The valid `// CHECK` block is multiline string that starts with the `// CHECK` comment optionally followed by a whitespace-separated list
 * of [TargetBackend] names, and ending with a colon. After that, an arbitrary number of single-line comments may follow. The `// CHECK` block
 * ends with the first non-comment line.
 *
 * For example, this text
 * ```kotlin
 * // CHECK JS_IR NATIVE:
 * //   Mangled name: #test(){}
 * //   Public signature: /test|6620506149988718649[0]
 * fun test(): Int {
 *   return 42
 * }
 * ```
 * will be parsed into:
 * ```kotlin
 * CheckBlock(
 *   backends = listOf(TargetBackend.JS_IR, TargetBackend.NATIVE),
 *   expectations = listOf(
 *     "//   Mangled name: #test(){}",
 *     "//   Public signature: /test|6620506149988718649[0]"
 *   )
 * )
 * ```
 *
 * @param trimmedCheckLine The line that starts with `// CHECK <BACKEND>*:`
 * @param lineIterator The iterator over lines in the expectation file.
 * @return The line representing the beginning of the next `// CHECK` block (if there is one), and the parsed `// CHECK` block.
 */
private fun parseSingleCheckBlock(trimmedCheckLine: String, lineIterator: Iterator<String>): Pair<String?, CheckBlock> {
    assert(trimmedCheckLine.startsWith(CHECK_MARKER))
    val colonIndex = trimmedCheckLine.indexOf(':')
    if (colonIndex < 0) {
        error("Expected colon after '$CHECK_MARKER' in '$trimmedCheckLine'")
    }
    val backends = trimmedCheckLine
        .substring(CHECK_MARKER.length, colonIndex)
        .splitToSequence(whitespaceRegex)
        .filter { it.isNotEmpty() }
        .map { enumValueOf<TargetBackend>(it) }
        .toList()
    val expectations = mutableListOf<String>()
    for (line in lineIterator) {
        val trimmed = line.trim()
        if (trimmed.startsWith(CHECK_MARKER)) {
            // Encountered the next // CHECK block
            return trimmed to CheckBlock(backends, expectations)
        }
        if (trimmed.startsWith("//")) {
            expectations.add(trimmed)
        } else {
            // The // CHECK block has ended, no more // CHECK blocks ahead in this group.
            break
        }
    }
    // Either we have no more lines, or the next line is not valid beginning of a `// CHECK` block.
    return null to CheckBlock(backends, expectations)
}

