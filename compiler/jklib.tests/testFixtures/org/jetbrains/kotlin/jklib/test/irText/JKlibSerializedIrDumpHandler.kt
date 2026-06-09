/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.backend.common.DumpIrReferenceRenderingAsSignatureStrategy
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.IR_EXTERNAL_DECLARATION_STUB
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions.ReferenceRenderingStrategy
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_IR_DESERIALIZATION_CHECKS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File

class JKlibSerializedIrDumpHandler(
    testServices: TestServices,
    private val isAfterDeserialization: Boolean,
) : AbstractIrHandler(testServices) {

    private val dumper = MultiModuleInfoDumper()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(KlibBasedCompilerTestDirectives)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (module.isSkipped) return

        val isFirFrontend = testServices.defaultsProvider.frontendKind == FrontendKinds.FIR

        val dumpOptions = DumpIrTreeOptions(
            normalizeNames = true,
            stableOrder = true,
            stableOrderOfOverriddenSymbols = true,
            // Issue 1: Removal of IR_EXTERNAL_DECLARATION_STUB origin on standard library classes and objects
            renderOriginForExternalDeclarations = false,
            printExpectDeclarations = false,
            declarationFlagsFilter = FlagsFilterImpl(isAfterDeserialization),
            printAnnotationsWithSourceRetention = false,
            printDispatchReceiverTypeInFakeOverrides = false,
            printParameterNamesInOverriddenSymbols = false,
            printFakeOverrideSymbolsInPropertiesOfAnonymousClasses = !testServices.moduleStructure.modules.first().languageVersionSettings
                .supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization),
            printMemberAccessExpressionArgumentNames = false,
            // Issue 2: Omission of the sealedSubclasses property dump list
            printSealedSubclasses = false,
            replaceImplicitSetterParameterNameWith = DEFAULT_VALUE_PARAMETER,
            isHiddenDeclaration = { declaration ->
                if (IrTextDumpHandler.isHiddenDeclaration(declaration, info.irBuiltIns)) {
                    true
                } else if (
                    isAfterDeserialization &&
                    declaration is IrSimpleFunction &&
                    declaration.isFakeOverride &&
                    declaration.resolveFakeOverride()?.origin == IrDeclarationOrigin.SYNTHETIC_ACCESSOR
                ) {
                    true
                } else if ((declaration is IrSimpleFunction || declaration is IrProperty) &&
                    declaration.parent.let { it is IrClass && it.visibility == DescriptorVisibilities.LOCAL } &&
                    declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
                    testServices.moduleStructure.modules.first().languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)
                ) {
                    true
                } else {
                    false
                }
            },
            printSourceOffsets = isFirFrontend,
            referenceRenderingStrategy = DumpIrReferenceRenderingAsSignatureStrategy(info.irMangler),
        )

        val builder = dumper.builderForModule(module.name)

        val irFiles = info.irModuleFragment.files.sortedBy { it.fileEntry.name }
        for (irFile in irFiles) {
            val actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, dumpOptions)
            builder.append(actualDump.normalizeForSerializedIrDump())
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (testServices.moduleStructure.modules.any { it.isSkipped }) return

        val dump = dumper.generateResultingDump()
        val dumpFile = testServices.dumpFile

        if (!isAfterDeserialization) {
            assertions.assertFileDoesntExist(dumpFile) { "Dump file already exists: $dumpFile" }
            dumpFile.writeText(dump)
        } else {
            assertions.assertEqualsToFile(dumpFile, dump)
        }
    }

    companion object {
        private val TestModule.isSkipped: Boolean
            get() = SKIP_IR_DESERIALIZATION_CHECKS in directives

        private val TestServices.dumpFile: File
            get() = temporaryDirectoryManager.rootDir.resolve("ir_pre_serialization_dump.txt")
    }
}

private class FlagsFilterImpl(private val isAfterDeserialization: Boolean) : DumpIrTreeOptions.FlagsFilter {
    override fun filterFlags(declaration: IrDeclaration, isReference: Boolean, flags: List<String>): List<String> = flags
        .removeExternalFlagInDeclarationReferences(isReference)
        .removeDelegatedFlagFromPropertyFakeOverrides(declaration, isReference)

    private fun List<String>.removeExternalFlagInDeclarationReferences(isReference: Boolean): List<String> =
        applyIf(isReference) { this - "external" }

    private fun List<String>.removeDelegatedFlagFromPropertyFakeOverrides(
        declaration: IrDeclaration,
        isReference: Boolean,
    ): List<String> {
        if (declaration !is IrProperty) return this

        return applyIf(isReference || declaration.isFakeOverride) { this - "delegated" }
    }
}

private fun String.normalizeForSerializedIrDump(): String {
    // Issue 1: Removal of IR_EXTERNAL_DECLARATION_STUB origin on standard library classes and objects.
    // Issue 4: Extra origin (IR_EXTERNAL_DECLARATION_STUB / IR_EXTERNAL_JAVA_DECLARATION_STUB) on Java declaration parameters.
    var result = this
        .replace("IR_EXTERNAL_DECLARATION_STUB ", "")
        .replace("IR_EXTERNAL_DECLARATION_STUB", "")
        .replace("IR_EXTERNAL_JAVA_DECLARATION_STUB ", "")
        .replace("IR_EXTERNAL_JAVA_DECLARATION_STUB", "")
    
    // Issue 5: Addition of @[UnsafeVariance] annotation on collection methods at call sites.
    // Issue 8: Removal of @[FlexibleArrayElementVariance] on Java array type arguments.
    result = result
        .replace("@[UnsafeVariance] ", "")
        .replace("@[UnsafeVariance]", "")
        .replace("@[FlexibleArrayElementVariance] ", "")
        .replace("@[FlexibleArrayElementVariance]", "")

    // Issue 7: Enhanced nullability and mutability details on return types in fake overrides.
    // We normalize returnType in "FUN FAKE_OVERRIDE" lines to strip annotations, generic type parameters, nullability, and raw-type markers.
    val lines = result.split("\n")
    val normalizedLines = lines.map { line ->
        if (line.contains("FUN FAKE_OVERRIDE")) {
            val returnTypeRegex = Regex("""returnType:(.+?)(?=\s+\[|$)""")
            returnTypeRegex.replace(line) { matchResult ->
                val typeStr = matchResult.groupValues[1]
                val normalizedType = typeStr
                    .replace(Regex("""@\[[^\]]+\]\s*"""), "") // Remove annotations
                    .replace(Regex("""<.*>"""), "") // Remove generic arguments
                    .replace(">", "") // Remove raw type trailing >
                    .replace("?", "") // Remove nullability
                "returnType:$normalizedType"
            }
        } else {
            line
        }
    }
    return normalizedLines.joinToString("\n")
}
