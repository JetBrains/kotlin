package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.IrMangledNameAndSignatureDumpHandler.Companion.DUMP_EXTENSION
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.groupWithTestFiles
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SIGNATURES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_SIGNATURE_DUMP
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import java.io.FileNotFoundException

class IrSignatureDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : AbstractIrHandler(testServices, artifactKind) {
    /**
     * The file that stores the expected signatures.
     */
    private val testDataFileWithExpectedSignatures: File by lazy {
        testServices.moduleStructure.originalTestDataFiles.first().withExtension(DUMP_EXTENSION)
    }

    /**
     * The parsed `// CHECK` block groups in [testDataFileWithExpectedSignatures]. Used to find all public signatures.
     */
    private val checkBlockGroups: Collection<IrTextTestCheckBlockGroup> by lazy {
        try {
            parseAllIrTextTestCheckBlocks(testDataFileWithExpectedSignatures.readText())
        } catch (e: FileNotFoundException) {
            emptyList()
        }
    }

    private val expectedSignatures: MutableSet<String> = hashSetOf()
    private val actualSignatures: MutableSet<String> = hashSetOf()

    private fun computeExpectedSignatures(targetBackend: TargetBackend?) {
        for (checkBlockGroup in checkBlockGroups) {
            for (checkBlock in checkBlockGroup) {
                if (checkBlock.backends.isEmpty() || TargetBackend.ANY in checkBlock.backends || targetBackend in checkBlock.backends) {
                    // This // CHECK block corresponds to the current target backend.
                    for (expectation in checkBlock.expectations) {
                        expectation.removePrefix("//")
                            .trimStart(Char::isWhitespace)
                            .substringAfter("Public signature: ", missingDelimiterValue = "")
                            .takeIf(String::isNotBlank)
                            ?.let(expectedSignatures::add)
                    }
                }
            }
        }
    }

    private fun extractSignature(symbol: IrSymbol) {
        val signature = symbol.signature ?: return
        if (signature.isPubliclyVisible && !signature.isLocal)
            actualSignatures += signature.render(IdSignatureRenderer.LEGACY)
    }

    private fun extractSignatures(declaration: IrDeclaration) {
        if (declaration !is IrDeclarationWithVisibility) return

        when (declaration) {
            is IrTypeAlias -> extractSignature(declaration.symbol)
            is IrClass -> {
                extractSignature(declaration.symbol)
                for (member in declaration.declarations) {
                    extractSignatures(member)
                }
            }
            is IrFunction -> runIf(!declaration.isFakeOverriddenFromAny()) {
                extractSignature(declaration.symbol)
            }
            is IrProperty -> {
                extractSignature(declaration.symbol)
                declaration.getter?.let(::extractSignatures)
                declaration.setter?.let(::extractSignatures)
            }
            is IrEnumEntry -> extractSignature(declaration.symbol)
        }
    }

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_SIGNATURES !in module.directives || SKIP_SIGNATURE_DUMP in module.directives) return

        computeExpectedSignatures(module.targetBackend)

        info.processAllIrModuleFragments(module) { irModuleFragment, _ ->
            val irFiles = irModuleFragment.files
            val filteredIrFiles = irFiles.groupWithTestFiles(module).filterNot { (testFile, _) ->
                testFile?.let { EXTERNAL_FILE in it.directives || it.isAdditional } ?: false
            }.map { it.second }

            for (irFile in filteredIrFiles) {
                for (declaration in irFile.declarations) {
                    extractSignatures(declaration)
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (actualSignatures != expectedSignatures) {
            val expectedSignaturesFile = testServices.temporaryDirectoryManager.rootDir.resolve("expected.txt")
                .apply { writeText(expectedSignatures.sorted().joinToString("\n")) }
            val actualSignaturesText = actualSignatures.sorted().joinToString("\n")

            assertions.assertEqualsToFile(expectedSignaturesFile, actualSignaturesText)
        }

//        val frontendKind = testServices.defaultsProvider.defaultFrontend
//        val muteDirectives = listOfNotNull(
//            CodegenTestDirectives.MUTE_SIGNATURE_COMPARISON_K2.takeIf { frontendKind == FrontendKinds.FIR },
//        )
//        testServices.codegenSuppressionChecker.checkMuted<FileComparisonFailure>(muteDirectives) {
//            assertions.assertEqualsToFile(testDataFileWithExpectedSignatures, dumper.generateResultingDump())
//        }
    }
}
