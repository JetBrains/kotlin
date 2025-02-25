/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.IR_EXTERNAL_DECLARATION_STUB
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
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

/**
 * This is a special IR dump handler required to test absence of losses during IR serialization/IR deserialization
 * cycle in KLIB-based backends. It dumps the IR text to a temporary file immediately before serializing IR to a KLIB,
 * and then dumps the deserialized IR text and checks it against the previously written file.
 *
 * The typical usage of this handler is the following:
 * ```
 * irHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = false) }) }
 * facadeStep(<serializer>)
 * klibArtifactsHandlersStep()
 * facadeStep(<deserializer>)
 * deserializedIrHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) }) }
 * ```
 *
 * @property isAfterDeserialization Whether the handler is to be executed before serialization (false) or after deserialization (true).
 */
class SerializedIrDumpHandler(
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
            /** Rename temporary local variables using a stable naming scheme. */
            normalizeNames = true,

            /** Print declarations always in stable order. */
            stableOrder = true,

            /** Print overridden symbols always in stable order. */
            stableOrderOfOverriddenSymbols = true,

            /**
             * [IR_EXTERNAL_DECLARATION_STUB] origin is missing in deserialized IR, but it might be present in
             * symbol references in frontend-generated IR.
             */
            renderOriginForExternalDeclarations = isAfterDeserialization,

            /** Expect declarations do not survive during serialization to KLIB. */
            printExpectDeclarations = false,

            /** Filter out specific "conflicting" flags. */
            declarationFlagsFilter = FlagsFilterImpl(isAfterDeserialization),

            /** Don't dump annotations having source retention such as [UnsafeVariance], [Suppress], [OptIn]. */
            printAnnotationsWithSourceRetention = false,

            /**
             * Fake overrides generation works slightly different for Fir2LazyIr and normal IR (either built
             * from the compiled sources or deserialized). Dispatch receivers for the corresponding fake overrides
             * in those two groups of IR sometimes are not identical (though, always compatible).
             */
            printDispatchReceiverTypeInFakeOverrides = false,

            /**
             * Names of value parameters are not a part of ABI (except for the single existing case in Kotlin/Native,
             * which should be covered with a separate bunch of tests).
             *
             * Also, names of value parameters sometimes mismatch between the frontend-generated IR and the deserialized IR.
             * Example:
             * ```
             * abstract class A<T> {
             *     context(T) abstract fun foo(): Int?
             * }
             *
             * class B : A<String>() {
             *     // frontend-generated IR:
             *     // FUN name:foo visibility:public modality:OPEN <> ($this:<root>.B, $context_receiver_0:kotlin.String) returnType:kotlin.Int?
             *     //   overridden:
             *     //     public abstract fun foo (<unused var>: T of <root>.A): kotlin.Int? declared in <root>.A
             *     //
             *     // deserialized IR:
             *     // FUN name:foo visibility:public modality:OPEN <> ($this:<root>.B, $context_receiver_0:kotlin.String) returnType:kotlin.Int?
             *     //   overridden:
             *     //     public abstract fun foo ($context_receiver_0: T of <root>.A): kotlin.Int? declared in <root>.A
             *     context(String) override fun foo(): Int? = 1
             * }
             * ```
             */
            printParameterNamesInOverriddenSymbols = false,

            /**
             * Sealed subclasses are not deserialized from KLIBs, so we need to wipe them out from dumps.
             * For details see KT-54028,
             * and in particular the commit https://github.com/jetbrains/kotlin/commit/3713d95bb1fc0cc434eeed42a0f0adac52af091b
             */
            printSealedSubclasses = isAfterDeserialization,

            /**
             * Sometimes the value parameter of a property setter might have different name in lazy IR and in deserialized IR.
             *
             * Example:
             * ```
             * // Kotlin/JS
             * external var foo: String = definedExternally
             * ```
             * Before serialization (lazy IR):
             * ```
             * CALL 'public final fun <set-foo> (value: kotlin.String): kotlin.Unit [external] declared in <root>' type=kotlin.Unit origin=EQ
             *   value: CONST String type=kotlin.String value="foo"
             * ```
             * After deserialization:
             * ```
             * CALL 'public final fun <set-foo> (<set-?>: kotlin.String): kotlin.Unit [external] declared in <root>' type=kotlin.Unit origin=EQ
             *   <set-?>: CONST String type=kotlin.String value="foo"
             * ```
             * This difference in names does not influence the behavior of the compiler. However, it is notable in IR dumps.
             */
            replaceImplicitSetterParameterNameWith = DEFAULT_VALUE_PARAMETER,

            isHiddenDeclaration = { declaration ->
                if (IrTextDumpHandler.isHiddenDeclaration(declaration, info.irPluginContext.irBuiltIns)) {
                    /** Reuse the existing rules for filtering declarations as in IR text tests. */
                    true
                } else if (isAfterDeserialization &&
                    declaration is IrSimpleFunction &&
                    declaration.isFakeOverride &&
                    declaration.resolveFakeOverride()?.origin == IrDeclarationOrigin.SYNTHETIC_ACCESSOR
                ) {
                    /**
                     * Ignore fake overrides for synthetic accessors generated in the deserialized IR.
                     * There were no fake overrides for synthetic accessors in IR built by Fir2Ir.
                     */
                    true
                } else {
                    false
                }
            },

            /**
             * Render offsets, but only in case the FIR frontend is used.
             * There are some known mismatches in offsets of fake overrides between K1 LazyIr and deserialized IR,
             * which we don't care much.
             */
            printSourceOffsets = isFirFrontend,

            /**
             * A workaround for mismatched offsets in default value expressions in annotations of fake overrides,
             * which is finally going to be fixed in KT-74938.
             *
             * Example:
             * ```
             * // Fir2LazyIr:
             * FUN[138, 282] FAKE_OVERRIDE name:...
             *   annotations:
             *     Deprecated(message = "...", replaceWith = <null>, level = GET_ENUM[-1, -1] 'ENUM_ENTRY name:HIDDEN' type=kotlin.DeprecationLevel)
             *
             * // Deserialized IR:
             * FUN[138, 282] FAKE_OVERRIDE name:...
             *   annotations:
             *     Deprecated(message = "...", replaceWith = <null>, level = GET_ENUM[1899, 1905] 'ENUM_ENTRY name:HIDDEN' type=kotlin.DeprecationLevel)
             * ```
             */
            printAnnotationsInFakeOverrides = false,
        )

        val builder = dumper.builderForModule(module.name)

        val irFiles = info.irModuleFragment.files.sortedBy { it.fileEntry.name }
        for (irFile in irFiles) {
            val actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, dumpOptions)
            builder.append(actualDump)
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

    /**
     * The text of every rendered [IrDeclarationReference] expression contains some details about the referenced symbol and
     * its "owner" declaration. As a part of this text, there are declaration flags, one of which can be the "external" flag.
     * This flag is rendered if the declaration is marked as "external" (see [IrPossiblyExternalDeclaration.isExternal]).
     *
     * However, there is a difference in how "external"-ness is propagated to declarations in Fir2LazyIr and in
     * the IR during the serialization/deserialization. This makes it difficult to compare this flag 1-to-1 in the IR
     * that was before serialization and the deserialized IR.
     *
     * We know that there are two strict rules that allow correctly handling "external" flag in the compiler:
     * 1.Language permits `external` keyword only for top-level declarations. So, any top-level declaration must have this flag
     *   set in metadata and subsequently in IR proto.
     * 2.Whenever the compiler needs to check "external"-ness of a declaration, it should check the effective "external"-ness
     *   (i.e., taking into account containing declarations, if there are any).
     *
     * Given this, it makes sense to filter out any mentioning of this flag in IR expressions that may refer to symbols
     * bound to LazyIr/Fir2LazyIr declarations.
     */
    private fun List<String>.removeExternalFlagInDeclarationReferences(isReference: Boolean): List<String> =
        applyIf(isReference) { this - "external" }

    /**
     * Remove 'delegated' flag from property fake overrides and overridden symbols.
     *
     * Due to the differences in the fake overrides generation algorithm between Fir2Ir and regular IR,
     * it could happen that the attributes of the same fake override are computed differently.
     * As a result, the generated fake override could get different flags.
     *
     * Example 1:
     * ```
     * // module 1
     * open class A {
     *     open val x = 1
     * }
     *
     * open class B: A() {
     *     override val x by lazy { 2 }
     * }
     *
     * // module 2 (depends on module 1)
     * open class C: B() {
     *     // frontend-generated IR:
     *     // PROPERTY name:x visibility:public modality:OPEN [val]
     *     //   overridden:
     *     //     public open x: kotlin.Int [val] declared in B
     *     //
     *     // deserialized IR:
     *     // PROPERTY name:x visibility:public modality:OPEN [val]
     *     //   overridden:
     *     //     public open x: kotlin.Int [delegated,val] declared in B
     *     override val x = 3
     * }
     * ```
     *
     * Example 2:
     * ```
     * // module 1
     * open class B {
     *     val a by lazy { "b" }
     * }
     *
     * // module 2 (depends on module 1)
     * class C : B() {
     *     // frontend-generated IR:
     *     // PROPERTY FAKE_OVERRIDE name:a visibility:public modality:FINAL [fake_override,val]
     *     //   overridden:
     *     //     public final a: kotlin.String [val] declared in B
     *     //
     *     // deserialized IR:
     *     // PROPERTY FAKE_OVERRIDE name:a visibility:public modality:FINAL [delegated,fake_override,val]
     *     //   overridden:
     *     //     public final a: kotlin.String [delegated,val] declared in B
     *     val a = "c"
     * }
     * ```
     */
    private fun List<String>.removeDelegatedFlagFromPropertyFakeOverrides(
        declaration: IrDeclaration,
        isReference: Boolean,
    ): List<String> {
        if (declaration !is IrProperty) return this

        return applyIf(isReference || declaration.isFakeOverride) { this - "delegated" }
    }
}
