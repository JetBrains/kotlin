/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.ir.hasPlatformDependent
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.interpreter.intrinsicConstEvaluationAnnotation
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SIGNATURES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.MUTE_SIGNATURE_COMPARISON_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SEPARATE_SIGNATURE_DUMP_FOR_K2
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LINK_VIA_SIGNATURES_K1
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
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
import org.opentest4j.AssertionFailedError
import java.io.File
import java.io.FileNotFoundException

private const val CHECK_MARKER = "// CHECK"

/**
 * Note: This handler has a limited usage.
 * -  It does not make sense for Kotlin/JVM with exception for a special experimental mode in K1
 *  where the linkage is performed by signatures. See also [LINK_VIA_SIGNATURES_K1].
 * - For KLIB-based backends, it has been superseded by [KlibAbiDumpHandler].
 *
 * Prints a mangled name and an [IdSignature] for each declaration and compares the result with
 * an expected output in a `*.sig.kt.txt` file located next to the test file.
 *
 * Can be enabled by specifying the [DUMP_SIGNATURES] directive.
 *
 * Other useful directives:
 * - [MUTE_SIGNATURE_COMPARISON_K2] can be used for muting the comparison result on the K2 frontend. The comparison will
 *   still be performed, and if it succeeds, the test will fail with a message reminding you to unmute it.
 * - [SEPARATE_SIGNATURE_DUMP_FOR_K2] acts like the inverse of [FIR_IDENTICAL], but it only affects signature dumps. Usually, signature
 *   dumps generated from K1 and K2 must be identical, but sometimes there are reasons to use separate dumps — in that case specify this
 *   directive.
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
 * //   Public signature debug description: test(){}kotlin.Int
 * // CHECK JS_IR NATIVE:
 * //   Mangled name: #test(){}
 * //   Public signature: /test|6620506149988718649[0]
 * //   Public signature debug description: test(){}
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

        private fun separateSignatureDirectiveNotPresent(testServices: TestServices): Boolean {
            return SEPARATE_SIGNATURE_DUMP_FOR_K2 !in testServices.moduleStructure.allDirectives
        }
    }

    override val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = listOf(::IdenticalChecker)

    class IdenticalChecker(testServices: TestServices) : SimpleFirIrIdenticalChecker(testServices, trimLines = true) {
        override val dumpExtension: String
            get() = DUMP_EXTENSION

        override fun markedAsIdentical(): Boolean {
            return separateSignatureDirectiveNotPresent(testServices)
        }

        override fun processClassicFileIfContentIsIdentical(testDataFile: File) {
            simpleChecker.removeDirectiveFromClassicFileAndAssert(testDataFile, SEPARATE_SIGNATURE_DUMP_FOR_K2)
        }
    }

    private fun computeDumpExtension(): String {
        return if (
            testServices.defaultsProvider.defaultFrontend == FrontendKinds.ClassicFrontend ||
            separateSignatureDirectiveNotPresent(testServices)
        ) {
            DUMP_EXTENSION
        } else {
            "fir.$DUMP_EXTENSION"
        }
    }

    private val dumper = MultiModuleInfoDumper("// MODULE: %s")

    /**
     * The file that stores the expected signatures of the test file.
     */
    private val expectedFile: File by lazy {
        testServices.moduleStructure.originalTestDataFiles.first().withExtension(computeDumpExtension())
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
        if (DUMP_SIGNATURES !in module.directives) return

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
                    info.irPluginContext.irBuiltIns,
                ),
                printFilePath = false,
                printFakeOverridesStrategy = FakeOverridesStrategy.ALL_EXCEPT_ANY,
                bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES,
                printUnitReturnType = true,
                stableOrder = true,
                // Expect declarations exist in K1 IR just before serialization, but won't be serialized. Though, dumps should be same before and after
                printExpectDeclarations = module.languageVersionSettings.languageVersion.usesK2,
            ),
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) {
            assertions.assertFileDoesntExist(expectedFile, DUMP_SIGNATURES)
            return
        }
        val frontendKind = testServices.defaultsProvider.defaultFrontend
        val muteDirectives = listOfNotNull(
            MUTE_SIGNATURE_COMPARISON_K2.takeIf { frontendKind == FrontendKinds.FIR },
        )
        testServices.codegenSuppressionChecker.checkMuted<AssertionFailedError>(muteDirectives) {
            assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
        }
    }

    private inner class DumpStrategy(
        val module: TestModule,
        val irMangler: KotlinMangler.IrMangler,
        val descriptorMangler: KotlinMangler.DescriptorMangler?,
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

        private val signatureComposer = PublicIdSignatureComputer(irMangler)

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

            // Can dump mangled names and signatures (both computed by the FE) only if
            // this is effectively a non-private declaration.
            val canDumpMangledNameAndSignaturesComputedByFrontend =
                (declaration as? IrDeclarationWithVisibility)?.isEffectivelyPrivate() != true

            val signatures = mutableListOf<ComputedSignature>()
            val fullMangledNames = mutableListOf<ComputedMangledName>()
            val signatureMangledNames = mutableListOf<ComputedMangledName>()

            val signatureComputedFromIr = signatureComposer.inFile(declaration.fileOrNull?.symbol) {
                signatureComposer.computeSignature(declaration).also {
                    addSignatureTo(signatures, it, ComputedBy.IR, isPublic = true)
                }
            }

            irMangler.addFullMangledNameTo(fullMangledNames, declaration)
            irMangler.addSignatureMangledNameTo(signatureMangledNames, declaration, ComputedBy.IR)

            if (canDumpMangledNameAndSignaturesComputedByFrontend) {
                addSignatureTo(signatures, symbol.signature, ComputedBy.FE, isPublic = true)
                addSignatureTo(signatures, symbol.privateSignature, ComputedBy.FE, isPublic = false)

                descriptorMangler?.addSignatureMangledNameTo(signatureMangledNames, symbol.descriptor, ComputedBy.FE)
            }

            fun printActualMangledNamesAndSignatures() {
                printMangledNames(fullMangledNames, prefix = "Mangled name")

                if (canDumpMangledNameAndSignaturesComputedByFrontend) {
                    // Signature mangled names computed from descriptors, IR and FIR of declarations that are not
                    // effectively private must be all equal to the signature description, which we already print
                    // (see the printSignatures() function below). If this is not the case, print them separately.
                    if (signatureMangledNames.any { it.value != signatureComputedFromIr.asPublic()?.description.orEmpty() }) {
                        printMangledNames(signatureMangledNames, prefix = "Mangled name for the signature")
                    }
                }

                printSignatures(signatures)
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

        override fun willPrintElement(element: IrElement, container: IrDeclaration?, printer: Printer, options: KotlinLikeDumpOptions): Boolean {
            if (element !is IrDeclaration) return true
            if (element is IrAnonymousInitializer) return false

            if (element.isExpect && !options.printExpectDeclarations) return false

            // Don't print synthetic property-less fields for delegates and context receivers. Ex:
            // class Foo {
            //   private /* final field */ val contextReceiverField0: ContextReceiverType
            //   private /* final field */ val $$delegate_0: BaseClassType
            // }
            if (element is IrField && element.origin.isSynthetic) return false

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
                (element.isStatic || element.hasPlatformDependent())
            ) {
                return false
            }

            // Don't print declarations that are not printed in all IR text tests.
            if (IrTextDumpHandler.isHiddenDeclaration(element, irBuiltIns))
                return false

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
    JvmSymbols.FLEXIBLE_VARIANCE_ANNOTATION_FQ_NAME,
    JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME,
    StandardClassIds.Annotations.ContextFunctionTypeParams.asSingleFqName(),
    JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION,
    JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION,
    intrinsicConstEvaluationAnnotation,
)

private data class ComputedSignature(
    val computedBy: ComputedBy,
    val isPublic: Boolean,
    val value: String,
    val description: String?
)

private enum class ComputedBy(private val humanReadableDescription: String) {
    FE("Frontend"), IR("IR");

    override fun toString() = humanReadableDescription
}

private data class ComputedMangledName(
    val computedBy: ComputedBy,
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

private fun Printer.printSignatures(computedSignatures: List<ComputedSignature>) {
    val (publicSignatures, privateSignatures) = computedSignatures.partition { it.isPublic }
    printSignatures(publicSignatures, "Public signature")
    printSignatures(privateSignatures, "Private signature")
}

private fun Printer.printSignatures(signatures: List<ComputedSignature>, prefix: String) {
    val distinctSignatures = signatures.distinctBy { it.value to it.description }
    when (distinctSignatures.size) {
        0 -> Unit
        1 -> printSignature(distinctSignatures.single(), prefix)
        else -> {
            distinctSignatures.forEach {
                printSignature(it, "$prefix by ${it.computedBy}")
            }
        }
    }
}

private fun Printer.printSignature(computedSignature: ComputedSignature, prefix: String) {
    println("//   $prefix: ${computedSignature.value}")
    computedSignature.description?.let {
        println("//   $prefix debug description: $it")
    }
}

private fun Printer.printMangledNames(computedMangledNames: List<ComputedMangledName>, prefix: String) {
    val distinctNames = computedMangledNames.distinctBy { it.value }

    // If mangled names computed from all three representations (descriptors, IR, FIR) and modes match,
    // print just one mangled name.
    distinctNames.singleOrNull()?.let {
        println("//   $prefix: ${it.value}")
        return
    }

    if (distinctNames.same { it.compatibleMode }) {
        // If the mangled names differ only by the mangler used (but not the compatibility mode), print
        // only mangled names from each mangler.
        computedMangledNames
            .distinctBy { it.computedBy }
            .printAligned(this) { "//   $prefix by ${it.computedBy}: " }
    } else if (distinctNames.same { it.computedBy }) {
        // If the mangled names differ only by the compatibility mode used (but not the mangler), print
        // only mangled names for each compatibility mode.
        computedMangledNames
            .distinctBy { it.compatibleMode }
            .printAligned(this) { "//   $prefix (compatible mode: ${it.compatibleMode}): " }
    } else {
        // Otherwise, print the whole matrix.
        computedMangledNames
            .printAligned(this) { "//   $prefix by ${it.computedBy} (compatible mode: ${it.compatibleMode}): " }
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

private fun <Declaration> addMangledNameTo(
    collector: MutableList<ComputedMangledName>,
    computedBy: ComputedBy,
    declaration: Declaration,
    mangle: Declaration.(Boolean) -> String,
) {
    listOf(false, true).mapTo(collector) { compatibleMode ->
        val mangledName = try {
            declaration.mangle(compatibleMode)
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
        ComputedMangledName(computedBy, compatibleMode, mangledName)
    }
}

private fun addSignatureTo(
    collector: MutableList<ComputedSignature>,
    signature: IdSignature?,
    origin: ComputedBy,
    isPublic: Boolean,
) {
    if (signature != null) {
        // N.B. We do use IdSignatureRenderer.LEGACY because it renders public signatures with hashes which are
        // computed from mangled names. So no real need in testing IdSignatureRenderer.DEFAULT which renders mangled names
        // instead of hashes.
        collector += ComputedSignature(
            origin,
            isPublic,
            value = signature.render(IdSignatureRenderer.LEGACY),
            description = signature.asPublic()?.description
        )
    }
}

private fun <Declaration, Mangler : KotlinMangler<Declaration>> Mangler.addSignatureMangledNameTo(
    collector: MutableList<ComputedMangledName>,
    declaration: Declaration,
    computedBy: ComputedBy
) {
    addMangledNameTo(collector, computedBy, declaration) { signatureString(it) }
}

private fun KotlinMangler.IrMangler.addFullMangledNameTo(
    collector: MutableList<ComputedMangledName>,
    declaration: IrDeclaration,
) {
    addMangledNameTo(collector, ComputedBy.IR, declaration) { mangleString(it) }
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
 * //   Public signature debug description: test(){}
 * fun test(): Int
 * ```
 * will be parsed into:
 * ```kotlin
 * CheckBlock(
 *   backends = listOf(TargetBackend.JS_IR, TargetBackend.NATIVE),
 *   expectations = listOf(
 *     "//   Mangled name: #test(){}",
 *     "//   Public signature: /test|6620506149988718649[0]",
 *     "//   Public signature debug description: test(){}",
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
