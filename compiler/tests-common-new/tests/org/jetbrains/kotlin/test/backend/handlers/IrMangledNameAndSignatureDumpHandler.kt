/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.ir.hasPlatformDependent
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.utils.isEqualsInheritedFromAny
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.interpreter.intrinsicConstEvaluationAnnotation
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_LOCAL_DECLARATION_SIGNATURES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SIGNATURES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.MUTE_SIGNATURE_COMPARISON_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_SIGNATURE_DUMP
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.same
import java.io.File
import java.io.FileNotFoundException

private const val CHECK_MARKER = "// CHECK"

/**
 * Prints a mangled name and an [IdSignature] for each declaration and compares the result with
 * an expected output in a `*.sig.kt.txt` file located next to the test file.
 *
 * Can be enabled by specifying the [DUMP_SIGNATURES] directive, and disabled with the [SKIP_SIGNATURE_DUMP] test directive.
 *
 * Other useful directives:
 * - [MUTE_SIGNATURE_COMPARISON_K2] can be used for muting the comparison result on the K2 frontend. The comparison will
 *   still be performed, and if it succeeds, the test will fail with a message reminding you to unmute it.
 * - [DUMP_LOCAL_DECLARATION_SIGNATURES] enables printing signatures for local functions and classes.
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
 * fun test(): Int
 * ```
 *
 * Here, on the JVM backend the mangled names include the return value, while on the JS and Native backends
 * this is not the case, so we use two `// CHECK` blocks to reflect that fact.
 *
 * When running a test against some backend, we do the following.
 *
 * 1. In order to compare this expectation with the actual output, we first parse all the `// CHECK` block groups in
 *    the `*.sig.kt.txt` file.
 *
 * 2. While dumping the declarations, for each declaration that we want to print the mangled name and
 *    the signature for, we take the next parsed group, and iterate over `// CHECK` blocks in that group.
 *
 * 3. For each parsed `// CHECK` block the following logic is applied:
 *    - If the current `// CHECK` block includes the backend that we're running this test against, or doesn't include
 *      any backends, we print the actual mangled name and signature for this declaration.
 *    - Otherwise, we print the parsed `// CHECK` block as is.
 */
class IrMangledNameAndSignatureDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : AbstractIrHandler(testServices, artifactKind) {

    companion object {
        const val DUMP_EXTENSION = "sig.kt.txt"
    }

    private val dumper = MultiModuleInfoDumper("// MODULE: %s")

    /**
     * The file that stores the expected signatures of the test file.
     */
    private val expectedFile: File by lazy {
        testServices.moduleStructure.originalTestDataFiles.first().withExtension(DUMP_EXTENSION)
    }

    /**
     * Iterates over parsed `// CHECK` block groups in [expectedFile].
     *
     * Each element is a list of `// CHECK` blocks for a single declaration.
     */
    private val checkBlockGroupIterator: Iterator<CheckBlockGroup> by lazy {
        try {
            parseAllCheckBlocks(expectedFile.readText())
        } catch (e: FileNotFoundException) {
            emptyList()
        }.iterator()
    }

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_SIGNATURES !in module.directives || SKIP_SIGNATURE_DUMP in module.directives) return

        dumpModuleKotlinLike(
            module,
            testServices.moduleStructure.modules,
            info,
            dumper,
            KotlinLikeDumpOptions(
                customDumpStrategy = DumpStrategy(
                    module,
                    info.irMangler,
                    info.descriptorMangler,
                    info.firMangler,
                    info.irModuleFragment.irBuiltins,
                ),
                printFilePath = false,
                printFakeOverridesStrategy = FakeOverridesStrategy.ALL_EXCEPT_ANY,
                bodyPrintingStrategy = if (DUMP_LOCAL_DECLARATION_SIGNATURES in module.directives)
                    BodyPrintingStrategy.PRINT_ONLY_LOCAL_CLASSES_AND_FUNCTIONS
                else
                    BodyPrintingStrategy.NO_BODIES,
                printUnitReturnType = true,
                stableOrder = true,
            ),
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val frontendKind = testServices.defaultsProvider.defaultFrontend
        val muteDirectives = listOfNotNull(
            MUTE_SIGNATURE_COMPARISON_K2.takeIf { frontendKind == FrontendKinds.FIR },
        )
        testServices.codegenSuppressionChecker.checkMuted<FileComparisonFailure>(muteDirectives) {
            assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
        }
    }

    private inner class DumpStrategy(
        val module: TestModule,
        val irMangler: KotlinMangler.IrMangler,
        val descriptorMangler: KotlinMangler.DescriptorMangler,
        val firMangler: FirMangler?,
        val irBuiltIns: IrBuiltIns,
    ) : CustomKotlinLikeDumpStrategy {

        private val targetBackend: TargetBackend
            get() = module.targetBackend!!

        private val IrDeclaration.isFunctionWithNonUnitReturnType: Boolean
            get() = this is IrSimpleFunction && !returnType.isUnit()

        private val IrDeclaration.isMainFunction: Boolean
            get() = isTopLevel && this is IrSimpleFunction && name.asString() == "main"

        @ObsoleteDescriptorBasedAPI
        private val IrDeclaration.potentiallyHasDifferentMangledNamesDependingOnBackend: Boolean
            get() = isMainFunction ||
                    isFunctionWithNonUnitReturnType ||
                    (symbol.descriptor as? PropertyDescriptor)?.isJavaField == true ||
                    parent.let { it is IrDeclaration && it.potentiallyHasDifferentMangledNamesDependingOnBackend }

        private fun IrSimpleFunction.isHiddenEnumMethod() = allOverridden(includeSelf = true).any {
            it.dispatchReceiverParameter?.type?.classOrNull == irBuiltIns.enumClass && it.name in HIDDEN_ENUM_METHOD_NAMES
        }

        private fun Printer.printCheckMarkerForNewDeclaration() {
            printlnCheckMarker(
                when (targetBackend) {
                    // In most cases the mangled names and signatures generated for JS are the same as for Native.
                    TargetBackend.JS_IR, TargetBackend.NATIVE -> listOf(TargetBackend.JS_IR, TargetBackend.NATIVE)
                    else -> listOf(targetBackend)
                }
            )
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        private fun Printer.printSignatureAndMangledName(declaration: IrDeclaration) {

            val symbol = declaration.symbol

            val computedMangledNames = mutableListOf<ComputedMangledName>()

            irMangler.addMangledNameTo(computedMangledNames, declaration)
            descriptorMangler.addMangledNameTo(computedMangledNames, symbol.descriptor)
            ((declaration as? IrMetadataSourceOwner)?.metadata as? FirMetadataSource)?.fir?.let {
                firMangler?.addMangledNameTo(
                    computedMangledNames,
                    it
                )
            }

            fun IdSignature.print(name: String) {
                val commentPrefix = "//   "
                // N.B. We do use IdSignatureRenderer.LEGACY because it renders public signatures with hashes which are
                // computed from mangled names. So no real need in testing IdSignatureRenderer.DEFAULT which renders mangled names
                // instead of hashes.
                println(commentPrefix, name, ": ", render(IdSignatureRenderer.LEGACY))
                asPublic()?.description?.let {
                    println(commentPrefix, name, " debug description: ", it)
                }
            }

            fun printActualMangledNamesAndSignatures() {
                printMangledNames(computedMangledNames)

                symbol.signature?.print("Public signature")
                symbol.privateSignature?.print("Private signature")
            }

            var printedActualMangledNameAndSignature = false
            if (checkBlockGroupIterator.hasNext()) {
                val checkBlockGroupForDeclaration = checkBlockGroupIterator.next()
                for (checkBlock in checkBlockGroupForDeclaration) {
                    printlnCheckMarker(checkBlock.backends)
                    if (checkBlock.backends.isEmpty() || TargetBackend.ANY in checkBlock.backends || targetBackend in checkBlock.backends) {
                        // If this // CHECK block corresponds to the current target backend, print the actual values
                        printActualMangledNamesAndSignatures()
                        printedActualMangledNameAndSignature = true
                    } else {
                        // Otherwise, print the // CHECK block from the expectation file as is
                        checkBlock.expectations.forEach(this::println)
                    }
                }
                // No `// CHECK` block found for the current backend.
                if (!printedActualMangledNameAndSignature) {
                    printCheckMarkerForNewDeclaration()
                    printActualMangledNamesAndSignatures()
                }
            } else {
                if (declaration.potentiallyHasDifferentMangledNamesDependingOnBackend) {
                    // This is a heuristic to print `// CHECK <backend>:` instead of just `// CHECK:` for new declarations
                    // for which the difference between backend-specific manglers is known to take place.
                    // This is purely for convenience when adding a new test.
                    printCheckMarkerForNewDeclaration()
                } else {
                    printlnCheckMarker(emptyList())
                }
                printActualMangledNamesAndSignatures()
            }
        }

        override fun willPrintElement(element: IrElement, container: IrDeclaration?, printer: Printer): Boolean {
            if (element !is IrDeclaration) return true
            if (element is IrAnonymousInitializer) return false

            // Don't print fake overrides of Java fields
            if (element is IrProperty &&
                element.isFakeOverride &&
                element
                    .collectRealOverrides()
                    .all { it.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && it.backingField != null }
            ) {
                return false
            }

            // Don't print certain fake overrides coming from Java classes
            if (element is IrSimpleFunction &&
                element.isFakeOverride &&
                (element.isStatic || element.hasPlatformDependent() || element.isHiddenEnumMethod())
            ) {
                return false
            }

            printer.printSignatureAndMangledName(element)

            return true
        }

        override fun shouldPrintAnnotation(annotation: IrConstructorCall, container: IrAnnotationContainer): Boolean =
            annotation.symbol.owner.constructedClass.kotlinFqName !in EXCLUDED_ANNOTATIONS

        override fun transformModifiersForDeclaration(
            declaration: IrDeclaration,
            modifiers: CustomKotlinLikeDumpStrategy.Modifiers
        ) = modifiers.copy(
            // Inner classes inside enum entries have local visibility on K2, but not on K1. Unify.
            visibility = if (declaration is IrClass && declaration.isInner && declaration.parent.let { it is IrClass && it.isEnumEntry }) {
                DescriptorVisibilities.LOCAL
            } else modifiers.visibility,
            // K2 doesn't always print 'external' modifier, unlike K1. Unify.
            isExternal = if (declaration is IrPossiblyExternalDeclaration) {
                declaration.isExternal || declaration.parents.any { it is IrPossiblyExternalDeclaration && it.isExternal }
            } else modifiers.isExternal,
        )
    }
}

/**
 * The annotations that are known not to affect signature computation but that may be generated inconsistently on K1 and K2.
 */
private val EXCLUDED_ANNOTATIONS = setOf(
    StandardNames.FqNames.deprecated,
    StandardNames.FqNames.unsafeVariance,
    StandardNames.FqNames.suppress,
    StandardNames.FqNames.parameterName,
    JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME,
    JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME,
    JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME,
    StandardClassIds.Annotations.ContextFunctionTypeParams.asSingleFqName(),
    JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION,
    JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION,
    intrinsicConstEvaluationAnnotation,
)

private val HIDDEN_ENUM_METHOD_NAMES = setOf(Name.identifier("finalize"), Name.identifier("getDeclaringClass"))

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
