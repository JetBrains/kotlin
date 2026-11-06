/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions.FlagsFilter
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions.ReferenceRenderingStrategy
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.dumpOrFail
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_EXTERNAL_CLASS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_DIFFERENCE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.independentSourceDirectoryPath
import org.jetbrains.kotlin.test.services.independentSourceDirectoryPathsTransitive
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class IrTextDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
    val customExtension: String? = null,
    val directive: SimpleDirective = DUMP_IR,
    val directiveForIrDifference: ValueDirective<TargetBackend> = DUMP_IR_DIFFERENCE,
    val showOffsets: Boolean = false,
    val renderReferencesAsSignatures: Boolean = false,
) : AbstractIrHandler(testServices, artifactKind) {
    companion object {
        const val DUMP_EXTENSION = "ir.txt"

        fun List<IrFile>.groupWithTestFiles(testServices: TestServices, ordered: Boolean = false): List<Pair<Pair<TestModule, TestFile>?, IrFile>> {
            return mapNotNull { irFile ->
                val name = File(irFile.fileEntry.name).name
                val moduleAndFile = testServices.moduleStructure.modules.firstNotNullOfOrNull { module ->
                    val file = module.files.firstOrNull { it.name == name } ?: return@firstNotNullOfOrNull null
                    module to file
                }
                moduleAndFile to irFile
            }.applyIf(ordered) {
                sortedBy { [moduleAndFile, irFile] ->
                    val pathFromIrFile = irFile.fileEntry.name
                    val [module, _] = moduleAndFile ?: return@sortedBy pathFromIrFile
                    pathFromIrFile.removePrefix(module.independentSourceDirectoryPath(testServices))
                }
            }
        }

        private val HIDDEN_ENUM_METHOD_NAMES = setOf(
            Name.identifier("finalize"), // JVM-specific fake override from java.lang.Enum. TODO: remove it after fixing KT-63744
            Name.identifier("getDeclaringClass"), // JVM-specific fake override from java.lang.Enum. TODO: remove it after fixing KT-63744
            Name.identifier("clone"), // JVM-specific fake override from kotlin.Enum (not java.lang.Enum !).
        )

        private fun IrSimpleFunction.isHiddenEnumMethod(irBuiltIns: IrBuiltIns): Boolean {
            return isFakeOverride && allOverridden(includeSelf = true).any {
                it.dispatchReceiverParameter?.type?.classOrNull == irBuiltIns.enumClass && it.name in HIDDEN_ENUM_METHOD_NAMES
            }
        }

        fun isHiddenDeclaration(declaration: IrDeclaration, irBuiltIns: IrBuiltIns): Boolean =
            (declaration as? IrSimpleFunction)?.isHiddenEnumMethod(irBuiltIns) == true

        fun renderFilePathForIrFile(
            testFileToIrFile: List<Pair<Pair<TestModule, TestFile>?, IrFile>>,
            testServices: TestServices,
            irFileEntry: IrFileEntry,
            fullPath: String,
        ): String {
            val [correspondingModule, _] = testFileToIrFile.firstOrNull { it.second.fileEntry == irFileEntry }?.first ?: return fullPath
            return fullPath.removePrefix(correspondingModule.independentSourceDirectoryPath(testServices))
        }
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDumpDirectives, CodegenTestDirectives)

    private val pathRelativizer = IrFileEntryPathRelativizer(testServices)

    private val baseDumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (directive !in module.directives) return

        pathRelativizer.addModule(module)

        val ignoreIrExpectFlag = CodegenTestDirectives.IGNORE_IR_EXPECT_FLAG in module.directives

        val dumpOptions = DumpIrTreeOptions(
            normalizeNames = true,
            printFacadeClassInFqNames = false,
            declarationFlagsFilter = FlagsFilter { declaration, isReference, flags ->
                // By coincidence, there is a huge number of cases in IR text test data files
                // when flags are still rendered for references to fields and classes.
                var filteredFlags = flags.takeIf { !isReference || declaration is IrField || declaration is IrClass }.orEmpty()
                if (ignoreIrExpectFlag && filteredFlags.isNotEmpty()) {
                    filteredFlags = filteredFlags.filter { it != "expect" }
                }
                filteredFlags
            },
            isHiddenDeclaration = { isHiddenDeclaration(it, info.irBuiltIns) },
            stableOrder = true,
            filePathRenderer = { _, fullPath ->
                pathRelativizer.getRelativePath(fullPath)
            },
            printSourceOffsets = showOffsets,
            referenceRenderingStrategy = if (renderReferencesAsSignatures)
                ClassAndCallableReferenceRenderingStrategy(info.irMangler, pathRelativizer)
            else
                ReferenceRenderingStrategy.Default,
        )

        val builder = baseDumper.builderForModule(module.name)

        for ([moduleAndFile, irFile] in info.irModuleFragment.files.groupWithTestFiles(testServices, ordered = true)) {
            val testFile = moduleAndFile?.second
            if (testFile?.directives?.contains(EXTERNAL_FILE) == true || moduleAndFile?.second?.isAdditional == true) continue
            val actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, dumpOptions)
            builder.append(actualDump)
        }

        compareDumpsOfExternalClasses(module, info)
    }

    private val externalFiles = mutableMapOf<File, String>()

    private fun compareDumpsOfExternalClasses(module: TestModule, info: IrBackendInput) {
        val externalClassIds = module.directives[DUMP_EXTERNAL_CLASS]
        if (externalClassIds.isEmpty()) return
        val dumpOptions = DumpIrTreeOptions(stableOrder = true, printFilePath = false)
        val baseFile = testServices.moduleStructure.originalTestDataFiles.first()
        assertions.assertAll(
            externalClassIds.map { externalClassId ->
                {
                    val classDump = info.findExternalClass(externalClassId).dumpOrFail(dumpOptions)
                    val suffix = ".__${externalClassId.replace("/", ".")}"
                    val expectedFile = baseFile.withSuffixAndExtension(suffix, getDumpExtension())
                    assertions.assertEqualsToFile(expectedFile, classDump)

                    val classBaseDump = baseFile.withSuffixAndExtension(suffix, getBaseDumpExtension())
                    externalFiles[classBaseDump] = classDump
                }
            }
        )
    }

    private fun IrBackendInput.findExternalClass(externalClassId: String): IrClass {
        val classId = ClassId.fromString(externalClassId)

        @OptIn(InternalSymbolFinderAPI::class)
        return irBuiltIns.symbolFinder.findClass(classId)?.owner
            ?: testInfraError( "Can't find a class in external dependencies: $externalClassId" )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val actualDump = baseDumper.generateResultingDump()
        val baseDumpExtension = getBaseDumpExtension()

        validateTargetSpecificDumpFile(
            testServices, assertions,
            baseDumpExtension = baseDumpExtension,
            directiveForIrDifference,
            actualDump,
            isKotlinLikeDump = false,
            externalFilesDumps = externalFiles,
        )
    }

    private fun getBaseDumpExtension(): String {
        return customExtension ?: DUMP_EXTENSION
    }

    private fun getDumpExtension(): String {
        return customExtension
            ?: getTargetSpecificDumpExtension(testServices, getBaseDumpExtension())
            ?: getBaseDumpExtension()
    }
}

private class IrFileEntryPathRelativizer(private val testServices: TestServices) {
    private val absolutePathPrefixes = linkedSetOf<String>()
    private val relativizedPathsCache = mutableMapOf<String, String>()

    fun addModule(module: TestModule) {
        absolutePathPrefixes.addAll(module.independentSourceDirectoryPathsTransitive(testServices))
    }

    fun getRelativePath(fullPath: String): String = relativizedPathsCache.getOrPut(fullPath) {
        absolutePathPrefixes.firstNotNullOfOrNull { absolutePathPrefix ->
            runIf(fullPath.startsWith(absolutePathPrefix)) { fullPath.removePrefix(absolutePathPrefix) }
        } ?: fullPath
    }
}

private class ClassAndCallableReferenceRenderingStrategy(
    mangler: KotlinMangler.IrMangler,
    private val pathRelativizer: IrFileEntryPathRelativizer,
) : ReferenceRenderingStrategy.Custom {
    private val signatureComputer = PublicIdSignatureComputer(mangler)

    override fun renderReference(symbol: IrSymbol): String? {
        if (!symbol.isClassOrCallable()) {
            // This might be a symbol of a value parameter, type parameter, variable, etc.
            // I.e. the kind of a symbol that normally can't be unbound after IR inliner.
            return null // Render it in a usual way.
        }

        // Otherwise: extract the signature (if it's there) or compute the signature on the fly.
        val signature: IdSignature = symbol.signature ?: symbol.computeSignature() ?: return null

        return signature.relativizeFilePaths().render()
    }

    private fun IrSymbol.isClassOrCallable(): Boolean =
        this is IrClassSymbol || this is IrEnumEntrySymbol || this is IrFunctionSymbol || this is IrPropertySymbol

    private fun IrSymbol.computeSignature(): IdSignature? {
        if (!isBound) return null
        val declaration = owner as? IrDeclaration ?: return null
        return when (val fileSymbol = declaration.fileOrNull?.symbol) {
            null -> signatureComputer.computeSignature(declaration)
            else -> signatureComputer.inFile(fileSymbol) { signatureComputer.computeSignature(declaration) }
        }
    }

    private fun IdSignature.relativizeFilePaths(): IdSignature {
        return when (this) {
            is IdSignature.CommonSignature, is IdSignature.LocalSignature, is IdSignature.ScopeLocalDeclaration -> this

            is IdSignature.FileSignature -> IdSignature.FileSignature(
                id = Any(), // does not matter
                fqName = packageFqName(),
                fileName = pathRelativizer.getRelativePath(fileName), // keep the path relative (and thus stable)
            )

            is IdSignature.AccessorSignature -> IdSignature.AccessorSignature(
                propertySignature = propertySignature.relativizeFilePaths(),
                accessorSignature = accessorSignature,
            )

            is IdSignature.CompositeSignature -> IdSignature.CompositeSignature(
                container = container.relativizeFilePaths(),
                inner = inner.relativizeFilePaths(),
            )

            is IdSignature.FileLocalSignature -> IdSignature.FileLocalSignature(
                container = container.relativizeFilePaths(),
                id = id,
            )

            is IdSignature.SpecialFakeOverrideSignature, is IdSignature.LoweredDeclarationSignature -> {
                // There are the special types of signatures that should not happen on 1st stage
                // of compilation in KLIB-based compilers.
                error("Should not happen: ${this::class.java}, ${this.render()}")
            }
        }
    }
}
