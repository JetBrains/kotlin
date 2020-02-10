/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.org.objectweb.asm.Type

class JvmSignatureClashDetector(
    private val irClass: IrClass,
    private val type: Type,
    private val context: JvmBackendContext
) {
    private val methodsBySignature = HashMap<RawSignature, MutableSet<IrFunction>>()
    private val fieldsBySignature = HashMap<RawSignature, MutableSet<IrField>>()

    fun trackField(irField: IrField, rawSignature: RawSignature) {
        fieldsBySignature.getOrPut(rawSignature) { SmartSet.create() }.add(irField)
    }

    fun trackMethod(irFunction: IrFunction) {
        if (irFunction is IrSimpleFunction && irFunction.isFakeOverride && irFunction.dispatchReceiverParameter != null) {
            for (overriddenFunction in getOverriddenFunctions(irFunction)) {
                trackMethod(irFunction, mapRawSignature(overriddenFunction))
            }
        } else {
            trackMethod(irFunction, mapRawSignature(irFunction))
        }
    }

    private fun mapRawSignature(irFunction: IrFunction): RawSignature {
        val jvmSignature = context.methodSignatureMapper.mapSignatureSkipGeneric(irFunction)
        return RawSignature(jvmSignature.asmMethod.name, jvmSignature.asmMethod.descriptor, MemberKind.METHOD)
    }

    private fun trackMethod(irFunction: IrFunction, rawSignature: RawSignature) {
        methodsBySignature.getOrPut(rawSignature) { SmartSet.create() }.add(irFunction)
    }

    private fun getOverriddenFunctions(irFunction: IrSimpleFunction): Set<IrFunction> {
        val result = HashSet<IrFunction>()
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

    fun reportErrors(classOrigin: JvmDeclarationOrigin) {
        // Class IFoo$DefaultImpls has conflicting signatures if and only if corresponding interface IFoo has conflicting signatures.
        // Do not report these diagnostics twice.
        if (irClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) return

        reportMethodSignatureConflicts(classOrigin)
        reportPredefinedMethodSignatureConflicts(classOrigin)
        reportFieldSignatureConflicts(classOrigin)
    }

    private fun reportMethodSignatureConflicts(classOrigin: JvmDeclarationOrigin) {
        for ((rawSignature, methods) in methodsBySignature) {
            if (methods.size <= 1) continue

            val fakeOverridesCount = methods.count { it.isFakeOverride }
            val specialOverridesCount = methods.count { it.isSpecialOverride() }
            val realMethodsCount = methods.size - fakeOverridesCount - specialOverridesCount

            val conflictingJvmDeclarationsData = getConflictingJvmDeclarationsData(classOrigin, rawSignature, methods)

            when {
                realMethodsCount == 0 && (fakeOverridesCount > 1 || specialOverridesCount > 1) ->
                    reportJvmSignatureClash(
                        ErrorsJvm.CONFLICTING_INHERITED_JVM_DECLARATIONS,
                        listOf(irClass),
                        conflictingJvmDeclarationsData
                    )

                fakeOverridesCount == 0 && specialOverridesCount == 0 ->
                    reportJvmSignatureClash(
                        ErrorsJvm.CONFLICTING_JVM_DECLARATIONS,
                        methods,
                        conflictingJvmDeclarationsData
                    )

                else ->
                    reportJvmSignatureClash(
                        ErrorsJvm.ACCIDENTAL_OVERRIDE,
                        methods.filter { !it.isFakeOverride && !it.isSpecialOverride() },
                        conflictingJvmDeclarationsData
                    )
            }
        }
    }

    private fun reportPredefinedMethodSignatureConflicts(classOrigin: JvmDeclarationOrigin) {
        for (predefinedSignature in PREDEFINED_SIGNATURES) {
            val knownMethods = methodsBySignature[predefinedSignature] ?: continue
            val methods = knownMethods.filter { !it.isFakeOverride && !it.isSpecialOverride() }
            if (methods.isEmpty()) continue
            val conflictingJvmDeclarationsData = ConflictingJvmDeclarationsData(
                type.internalName, classOrigin, predefinedSignature,
                methods.map { it.getJvmDeclarationOrigin() } + JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, null)
            )
            reportJvmSignatureClash(ErrorsJvm.ACCIDENTAL_OVERRIDE, methods, conflictingJvmDeclarationsData)
        }
    }

    private fun reportFieldSignatureConflicts(classOrigin: JvmDeclarationOrigin) {
        for ((rawSignature, fields) in fieldsBySignature) {
            if (fields.size <= 1) continue
            val conflictingJvmDeclarationsData = getConflictingJvmDeclarationsData(classOrigin, rawSignature, fields)
            reportJvmSignatureClash(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS, fields, conflictingJvmDeclarationsData)
        }
    }

    private fun reportJvmSignatureClash(
        diagnosticFactory1: DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData>,
        irDeclarations: Collection<IrDeclaration>,
        conflictingJvmDeclarationsData: ConflictingJvmDeclarationsData
    ) {
        val psiElements = irDeclarations.mapNotNullTo(HashSet()) { it.getElementForDiagnostics() }
        for (psiElement in psiElements) {
            context.psiErrorBuilder.at(psiElement)
                .report(diagnosticFactory1, conflictingJvmDeclarationsData)
        }
    }

    private fun IrDeclaration.findPsiElement() = context.psiSourceManager.findPsiElement(this)

    private fun IrDeclaration.getElementForDiagnostics(): PsiElement? =
        findPsiElement()
            ?: irClass.findPsiElement()

    private fun getConflictingJvmDeclarationsData(
        classOrigin: JvmDeclarationOrigin,
        rawSignature: RawSignature,
        methods: Collection<IrDeclaration>
    ): ConflictingJvmDeclarationsData =
        ConflictingJvmDeclarationsData(
            type.internalName,
            classOrigin,
            rawSignature,
            methods.map { it.getJvmDeclarationOrigin() }
        )

    private fun IrDeclaration.getJvmDeclarationOrigin(): JvmDeclarationOrigin {
        // It looks like 'JvmDeclarationOriginKind' is not really used in error reporting.
        // However, if needed, we can provide more meaningful information regarding function origin.
        return JvmDeclarationOrigin(
            JvmDeclarationOriginKind.OTHER,
            context.psiSourceManager.findPsiElement(this),
            descriptor
        )
    }

    companion object {
        val SPECIAL_BRIDGES_AND_OVERRIDES = setOf(
            IrDeclarationOrigin.BRIDGE,
            IrDeclarationOrigin.BRIDGE_SPECIAL,
            IrDeclarationOrigin.IR_BUILTINS_STUB,
            JvmLoweredDeclarationOrigin.TO_ARRAY,
            JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE,
            JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC
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
