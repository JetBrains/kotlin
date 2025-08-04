/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.linkage.issues.SignatureClashDetector
import org.jetbrains.kotlin.backend.common.lower.ANNOTATION_IMPLEMENTATION
import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.bridgeCallee
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.chooseFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.originalFunction
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.resolve.MemberComparator

class JvmMethodSignatureClashDetector(
    private val classCodegen: ClassCodegen
) : SignatureClashDetector<JvmMemberSignature.Method, IrFunction>() {

    private val overriddenByFakeOverrideBySignature: MutableMap<JvmMemberSignature.Method, MutableSet<IrFunction>> = HashMap()

    private fun overriddenByFakeOverrideWithSignature(signature: JvmMemberSignature.Method): Set<IrFunction> =
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

    private fun mapRawSignature(irFunction: IrFunction): JvmMemberSignature.Method {
        val jvmSignature = classCodegen.methodSignatureMapper.mapFakeOverrideSignatureSkipGeneric(irFunction)
        return JvmMemberSignature.Method(jvmSignature.asmMethod.name, jvmSignature.asmMethod.descriptor)
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

    override fun reportSignatureConflict(
        signature: JvmMemberSignature.Method,
        declarations: Collection<IrFunction>,
        diagnosticReporter: IrDiagnosticReporter
    ) {
        val fakeOverridesCount = declarations.count { it.isFakeOverride }
        val specialOverridesCount = declarations.count { it.isSpecialOverride() }
        val realMethodsCount = declarations.size - fakeOverridesCount - specialOverridesCount

        val conflictingJvmDeclarationsData = JvmIrConflictingDeclarationsData(signature, declarations)

        when {
            realMethodsCount == 0 && fakeOverridesCount == 1 && specialOverridesCount == 1 -> {
                val bridge = declarations.single { it.isSpecialOverride() }
                val bridgeCallee = bridge.bridgeCallee.takeIf { bridge.origin == IrDeclarationOrigin.BRIDGE }
                if (bridgeCallee is IrSimpleFunction &&
                    !getOverriddenFunctions(bridgeCallee).containsAll(overriddenByFakeOverrideWithSignature(signature))
                ) {
                    val diagnosticFactory = JvmBackendErrors.ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD.chooseFactory(
                        diagnosticReporter.at(classCodegen.irClass)
                    )
                    reportJvmSignatureClash(
                        diagnosticReporter,
                        diagnosticFactory,
                        listOf(classCodegen.irClass),
                        conflictingJvmDeclarationsData,
                    )
                }
            }

            realMethodsCount == 0 && (fakeOverridesCount > 1 || specialOverridesCount > 1) ->
                if (classCodegen.irClass.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
                    reportJvmSignatureClash(
                        diagnosticReporter,
                        JvmBackendErrors.CONFLICTING_INHERITED_JVM_DECLARATIONS,
                        listOf(classCodegen.irClass),
                        conflictingJvmDeclarationsData
                    )
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
            reportJvmSignatureClash(
                diagnosticReporter, JvmBackendErrors.ACCIDENTAL_OVERRIDE, methods,
                JvmIrConflictingDeclarationsData(predefinedSignature, methods),
            )
        }
    }

    private fun reportJvmSignatureClash(
        diagnosticReporter: IrDiagnosticReporter,
        diagnosticFactory1: KtDiagnosticFactory1<String>,
        irDeclarations: Collection<IrDeclaration>,
        conflictingJvmDeclarationsData: JvmIrConflictingDeclarationsData
    ) {
        reportSignatureClashTo(
            diagnosticReporter,
            diagnosticFactory1,
            irDeclarations,
            conflictingJvmDeclarationsData.render(),
            // Offset can be negative (SYNTHETIC_OFFSET) for delegated members; report an error on the class in that case.
            reportOnIfSynthetic = { classCodegen.irClass },
        )
    }

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
            JvmMemberSignature.Method("getClass", "()Ljava/lang/Class;"),
            JvmMemberSignature.Method("notify", "()V"),
            JvmMemberSignature.Method("notifyAll", "()V"),
            JvmMemberSignature.Method("wait", "()V"),
            JvmMemberSignature.Method("wait", "(J)V"),
            JvmMemberSignature.Method("wait", "(JI)V"),
        )
    }
}

internal class JvmIrConflictingDeclarationsData(
    val signature: JvmMemberSignature,
    val declarations: Collection<IrDeclaration>,
) {
    fun render(): String = renderer.render(this)

    companion object {
        private val renderer = CommonRenderers.renderConflictingSignatureData(
            signatureKind = "JVM",
            sortUsing = MemberComparator.INSTANCE,
            declarationRenderer = Renderers.WITHOUT_MODIFIERS,
            renderSignature = {
                append(it.signature.name)
                append(it.signature.desc)
            },
            declarations = fun JvmIrConflictingDeclarationsData.() = declarations.map(IrDeclaration::toIrBasedDescriptor),
        )
    }
}
