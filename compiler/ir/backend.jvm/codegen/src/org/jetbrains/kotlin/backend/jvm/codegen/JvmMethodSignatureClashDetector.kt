/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.linkage.issues.SignatureClashDetector
import org.jetbrains.kotlin.backend.common.lower.ANNOTATION_IMPLEMENTATION
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

class JvmMethodSignatureClashDetector(
    private val classCodegen: ClassCodegen
) : SignatureClashDetector<RawSignature, IrFunction>() {

    private val overriddenByFakeOverrideBySignature: HashMap<RawSignature, MutableSet<IrFunction>> = LinkedHashMap()

    private fun overriddenByFakeOverrideWithSignature(signature: RawSignature): Set<IrFunction> =
        overriddenByFakeOverrideBySignature[signature] ?: emptySet()

    fun trackFakeOverrideMethod(irFunction: IrFunction) {
        if (irFunction.dispatchReceiverParameter == null) {
            trackDeclaration(irFunction, mapRawSignature(irFunction))
        } else {
            for (overriddenFunction in getOverriddenFunctions(irFunction as IrSimpleFunction)) {
                if (!overriddenFunction.isFakeOverride) trackFakeOverrideAndOverridden(irFunction, overriddenFunction)
            }
        }
    }

    private fun trackFakeOverrideAndOverridden(fakeOverride: IrFunction, overridden: IrFunction) {
        val signature = mapRawSignature(overridden)
        trackDeclaration(fakeOverride, signature)
        overriddenByFakeOverrideBySignature.getOrPut(signature) { mutableSetOf() }.add(overridden)
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

    override fun reportErrorsTo(diagnosticReporter: IrDiagnosticReporter) {
        super.reportErrorsTo(diagnosticReporter)
        reportPredefinedMethodSignatureConflicts(diagnosticReporter)
    }

    private val IrFunction.originalFunctionForBridge: IrSimpleFunction?
        get() {
            if (origin != IrDeclarationOrigin.BRIDGE) return null
            return originalFunction as? IrSimpleFunction
        }

    override fun reportSignatureConflict(
        signature: RawSignature,
        declarations: Collection<IrFunction>,
        diagnosticReporter: IrDiagnosticReporter
    ) {
        val fakeOverridesCount = declarations.count { it.isFakeOverride }
        val specialOverridesCount = declarations.count { it.isSpecialOverride() }
        val realMethodsCount = declarations.size - fakeOverridesCount - specialOverridesCount

        val conflictingJvmDeclarationsData = getConflictingJvmDeclarationsData(signature, declarations)

        fun reportConflictingInheritedJvmDeclarations() =
            reportJvmSignatureClash(
                diagnosticReporter,
                JvmBackendErrors.CONFLICTING_INHERITED_JVM_DECLARATIONS,
                listOf(classCodegen.irClass),
                conflictingJvmDeclarationsData
            )

        when {
            realMethodsCount == 0 && fakeOverridesCount == 1 && specialOverridesCount == 1 -> {
                declarations.first { it.isSpecialOverride() }.originalFunctionForBridge?.let { originalOfBridge ->
                    val overriddenByOriginalOfBridge = getOverriddenFunctions(originalOfBridge)
                    // We check that overridden function is its original here to ensure
                    // that it is not the function with hash in its name generated for inline classes.
                    overriddenByFakeOverrideWithSignature(signature).any { overridden ->
                        !overriddenByOriginalOfBridge.contains(overridden) && overridden == overridden.originalFunction
                    }.ifTrue {
                        reportConflictingInheritedJvmDeclarations()
                    }
                }
            }

            realMethodsCount == 0 && (fakeOverridesCount > 1 || specialOverridesCount > 1) ->
                if (classCodegen.irClass.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
                    reportConflictingInheritedJvmDeclarations()
                }

            fakeOverridesCount == 0 && specialOverridesCount == 0 -> {
                // In IFoo$DefaultImpls we should report errors only if there are private methods among conflicting ones
                // (otherwise such errors would be reported twice: once for IFoo and once for IFoo$DefaultImpls).
                if (classCodegen.irClass.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS ||
                    declarations.any { DescriptorVisibilities.isPrivate(it.visibility) }
                ) {
                    reportJvmSignatureClash(
                        diagnosticReporter,
                        JvmBackendErrors.CONFLICTING_JVM_DECLARATIONS,
                        declarations,
                        conflictingJvmDeclarationsData
                    )
                }
            }

            else ->
                if (classCodegen.irClass.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
                    reportJvmSignatureClash(
                        diagnosticReporter,
                        JvmBackendErrors.ACCIDENTAL_OVERRIDE,
                        declarations.filter { !it.isFakeOverride && !it.isSpecialOverride() },
                        conflictingJvmDeclarationsData
                    )
                }
        }
    }

    private fun reportPredefinedMethodSignatureConflicts(diagnosticReporter: IrDiagnosticReporter) {
        for (predefinedSignature in PREDEFINED_SIGNATURES) {
            val methods = declarationsWithSignature(predefinedSignature).filter { !it.isFakeOverride && !it.isSpecialOverride() }
            if (methods.isEmpty()) continue
            val conflictingJvmDeclarationsData = ConflictingJvmDeclarationsData(
                classCodegen.type.internalName, predefinedSignature, methods.map(IrFunction::toIrBasedDescriptor),
            )
            reportJvmSignatureClash(diagnosticReporter, JvmBackendErrors.ACCIDENTAL_OVERRIDE, methods, conflictingJvmDeclarationsData)
        }
    }

    private fun reportJvmSignatureClash(
        diagnosticReporter: IrDiagnosticReporter,
        diagnosticFactory1: KtDiagnosticFactory1<ConflictingJvmDeclarationsData>,
        irDeclarations: Collection<IrDeclaration>,
        conflictingJvmDeclarationsData: ConflictingJvmDeclarationsData
    ) {
        reportSignatureClashTo(
            diagnosticReporter,
            diagnosticFactory1,
            irDeclarations,
            conflictingJvmDeclarationsData,
            // Offset can be negative (SYNTHETIC_OFFSET) for delegated members; report an error on the class in that case.
            reportOnIfSynthetic = { classCodegen.irClass },
        )
    }

    private fun getConflictingJvmDeclarationsData(
        rawSignature: RawSignature,
        methods: Collection<IrDeclaration>
    ): ConflictingJvmDeclarationsData =
        ConflictingJvmDeclarationsData(classCodegen.type.internalName, rawSignature, methods.map(IrDeclaration::toIrBasedDescriptor))

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
