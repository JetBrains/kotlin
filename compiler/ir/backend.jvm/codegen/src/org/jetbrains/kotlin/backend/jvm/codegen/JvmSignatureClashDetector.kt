/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.lower.ANNOTATION_IMPLEMENTATION
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
import org.jetbrains.kotlin.resolve.jvm.diagnostics.MemberKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.RawSignature
import org.jetbrains.kotlin.utils.SmartSet

class JvmSignatureClashDetector(
    private val classCodegen: ClassCodegen
) {
    private val methodsBySignature = LinkedHashMap<RawSignature, MutableSet<IrFunction>>()
    private val fieldsBySignature = LinkedHashMap<RawSignature, MutableSet<IrField>>()

    fun trackField(irField: IrField, rawSignature: RawSignature) {
        fieldsBySignature.getOrPut(rawSignature) { SmartSet.create() }.add(irField)
    }

    fun trackMethod(irFunction: IrFunction, rawSignature: RawSignature) {
        methodsBySignature.getOrPut(rawSignature) { SmartSet.create() }.add(irFunction)
    }

    fun trackFakeOverrideMethod(irFunction: IrFunction) {
        if (irFunction.dispatchReceiverParameter != null) {
            for (overriddenFunction in getOverriddenFunctions(irFunction as IrSimpleFunction)) {
                trackMethod(irFunction, mapRawSignature(overriddenFunction))
            }
        } else {
            trackMethod(irFunction, mapRawSignature(irFunction))
        }
    }

    private fun mapRawSignature(irFunction: IrFunction): RawSignature {
        val jvmSignature = classCodegen.methodSignatureMapper.mapFakeOverrideSignatureSkipGeneric(irFunction)
        return RawSignature(jvmSignature.asmMethod.name, jvmSignature.asmMethod.descriptor, MemberKind.METHOD)
    }

    private fun getOverriddenFunctions(irFunction: IrSimpleFunction): Set<IrFunction> {
        val result = LinkedHashSet<IrFunction>()
        collectOverridesOf(irFunction, result)
        return result
    }

    private fun collectOverridesOf(irFunction: IrSimpleFunction, result: MutableSet<IrFunction>) {
        for (overriddenSymbol in irFunction.overriddenSymbols) {
            collectOverridesTree(overriddenSymbol.owner, result)
        }
    }

    private fun collectOverridesTree(irFunction: IrSimpleFunction, visited: MutableSet<IrFunction>) {
        if (!visited.add(irFunction)) return
        collectOverridesOf(irFunction, visited)
    }

    private fun IrFunction.isSpecialOverride(): Boolean =
        origin in SPECIAL_BRIDGES_AND_OVERRIDES

    fun reportErrors() {
        reportMethodSignatureConflicts()
        reportPredefinedMethodSignatureConflicts()
        reportFieldSignatureConflicts()
    }

    private fun reportMethodSignatureConflicts() {
        for ((rawSignature, methods) in methodsBySignature) {
            if (methods.size <= 1) continue

            val fakeOverridesCount = methods.count { it.isFakeOverride }
            val specialOverridesCount = methods.count { it.isSpecialOverride() }
            val realMethodsCount = methods.size - fakeOverridesCount - specialOverridesCount

            val conflictingJvmDeclarationsData = getConflictingJvmDeclarationsData(rawSignature, methods)

            when {
                realMethodsCount == 0 && (fakeOverridesCount > 1 || specialOverridesCount > 1) ->
                    if (classCodegen.irClass.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
                        reportJvmSignatureClash(
                            JvmBackendErrors.CONFLICTING_INHERITED_JVM_DECLARATIONS,
                            listOf(classCodegen.irClass),
                            conflictingJvmDeclarationsData
                        )
                    }

                fakeOverridesCount == 0 && specialOverridesCount == 0 -> {
                    // In IFoo$DefaultImpls we should report errors only if there are private methods among conflicting ones
                    // (otherwise such errors would be reported twice: once for IFoo and once for IFoo$DefaultImpls).
                    if (classCodegen.irClass.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS ||
                        methods.any { DescriptorVisibilities.isPrivate(it.visibility) }
                    ) {
                        reportJvmSignatureClash(
                            JvmBackendErrors.CONFLICTING_JVM_DECLARATIONS,
                            methods,
                            conflictingJvmDeclarationsData
                        )
                    }
                }

                else ->
                    if (classCodegen.irClass.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
                        reportJvmSignatureClash(
                            JvmBackendErrors.ACCIDENTAL_OVERRIDE,
                            methods.filter { !it.isFakeOverride && !it.isSpecialOverride() },
                            conflictingJvmDeclarationsData
                        )
                    }
            }
        }
    }

    private fun reportPredefinedMethodSignatureConflicts() {
        for (predefinedSignature in PREDEFINED_SIGNATURES) {
            val knownMethods = methodsBySignature[predefinedSignature] ?: continue
            val methods = knownMethods.filter { !it.isFakeOverride && !it.isSpecialOverride() }
            if (methods.isEmpty()) continue
            val conflictingJvmDeclarationsData = ConflictingJvmDeclarationsData(
                classCodegen.type.internalName, null, predefinedSignature, null, methods.map(IrFunction::toIrBasedDescriptor),
            )
            reportJvmSignatureClash(JvmBackendErrors.ACCIDENTAL_OVERRIDE, methods, conflictingJvmDeclarationsData)
        }
    }

    private fun reportFieldSignatureConflicts() {
        for ((rawSignature, fields) in fieldsBySignature) {
            if (fields.size <= 1) continue
            val conflictingJvmDeclarationsData = getConflictingJvmDeclarationsData(rawSignature, fields)
            reportJvmSignatureClash(JvmBackendErrors.CONFLICTING_JVM_DECLARATIONS, fields, conflictingJvmDeclarationsData)
        }
    }

    private fun reportJvmSignatureClash(
        diagnosticFactory1: KtDiagnosticFactory1<ConflictingJvmDeclarationsData>,
        irDeclarations: Collection<IrDeclaration>,
        conflictingJvmDeclarationsData: ConflictingJvmDeclarationsData
    ) {
        irDeclarations.mapNotNullTo(LinkedHashSet()) { irDeclaration ->
            classCodegen.context.ktDiagnosticReporter.atFirstValidFrom(irDeclaration, classCodegen.irClass, containingIrFile = irDeclaration.file)
        }.forEach {
            it.report(diagnosticFactory1, conflictingJvmDeclarationsData)
        }
    }

    private fun getConflictingJvmDeclarationsData(
        rawSignature: RawSignature,
        methods: Collection<IrDeclaration>
    ): ConflictingJvmDeclarationsData =
        ConflictingJvmDeclarationsData(
            classCodegen.type.internalName, null, rawSignature, null, methods.map(IrDeclaration::toIrBasedDescriptor),
        )

    companion object {
        val SPECIAL_BRIDGES_AND_OVERRIDES = setOf(
            IrDeclarationOrigin.BRIDGE,
            IrDeclarationOrigin.BRIDGE_SPECIAL,
            IrDeclarationOrigin.IR_BUILTINS_STUB,
            JvmLoweredDeclarationOrigin.TO_ARRAY,
            JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE,
            ANNOTATION_IMPLEMENTATION
        )

        val PREDEFINED_SIGNATURES = listOf(
            RawSignature("getClass", "()Ljava/lang/Class;", MemberKind.METHOD),
            RawSignature("notify", "()V", MemberKind.METHOD),
            RawSignature("notifyAll", "()V", MemberKind.METHOD),
            RawSignature("wait", "()V", MemberKind.METHOD),
            RawSignature("wait", "(J)V", MemberKind.METHOD),
            RawSignature("wait", "(JI)V", MemberKind.METHOD)
        )
    }
}
