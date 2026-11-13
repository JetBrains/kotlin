/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.backend.jvm.ir.isInCurrentModule
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.CONTINUATION_ASM_TYPE
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method

/**
 * Detects cases when static methods have weaker access privileges than methods with the same signature in super-class.
 * This is not necessary for static fields, because javac resolves them differently, and they do not have the same problem.
 */
class JvmStaticMethodWeakeningAccessPrivilegeDetector(private val classCodegen: ClassCodegen) {
    private val errorCases = mutableListOf<ErrorCase>()

    // Private functions have empty `overriddenSymbols`, so we cannot rely on it.
    // We also cannot rely on `overriddenSymbols` for non-private functions, because
    // `overriddenSymbols` does not include generated functions (`@JvmStatic`, etc.).
    private val superClassStaticMethodsBySignature: Map<JvmMemberSignature.Method, IrSimpleFunction> by lazy(LazyThreadSafetyMode.NONE) {
        classCodegen.irClass.getInheritedStaticFunctions().associateBy { function ->
            val signature = function.computeJvmSignature(classCodegen)
            JvmMemberSignature.Method(signature.name, signature.descriptor)
        }
    }

    fun trackDeclaration(irFunction: IrFunction, signature: JvmMemberSignature.Method) {
        if (irFunction !is IrSimpleFunction || !irFunction.isStatic) return
        val hidden = superClassStaticMethodsBySignature[signature] ?: return
        if (hidden.jvmVisibilityRank > irFunction.jvmVisibilityRank) {
            errorCases.add(ErrorCase(irFunction, hidden))
        }
    }

    fun reportErrorsTo(diagnosticReporter: IrDiagnosticReporter) {
        for ([irFunction, hidden] in errorCases) {
            val containingFile = irFunction.fileOrNull ?: continue
            // Report on the property unless the accessor itself has a weaker visibility, e.g., `protected set`.
            val declaration =
                irFunction.correspondingPropertySymbol?.owner?.takeIf { it.visibility == irFunction.visibility } ?: irFunction
            val reportOn = declaration.takeUnless { it.startOffset < 0 } ?: classCodegen.irClass
            diagnosticReporter.at(reportOn, containingFile).report(
                JvmBackendErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER,
                irFunction.visibility.delegate,
                (hidden.correspondingPropertySymbol?.owner ?: hidden).name,
                hidden.resolveFakeOverrideOrSelf().parentAsClass.name,
            )
        }
    }
}

private data class ErrorCase(val irFunction: IrSimpleFunction, val hidden: IrSimpleFunction)

private fun IrClass.getInheritedStaticFunctions(): List<IrSimpleFunction> {
    val superClass = this.superClass ?: return emptyList()
    return superClass.getInheritableStaticFunctions().filter { !it.isJvmPackagePrivate || superClass.packageFqName == this.packageFqName }
}

private fun IrClass.getInheritableStaticFunctions(): List<IrSimpleFunction> {
    // Filter out fake overrides, because we manually handle static functions from superclass
    return getInheritedStaticFunctions() + simpleFunctions().filter { it.isStatic && !it.isJvmPrivate && !it.isFakeOverride }
}

private val IrDeclarationWithVisibility.isJvmPrivate: Boolean
    get() = getVisibilityAccessFlag() == Opcodes.ACC_PRIVATE

private val IrDeclarationWithVisibility.isJvmPackagePrivate: Boolean
    get() = getVisibilityAccessFlag() == AsmUtil.NO_FLAG_PACKAGE_PRIVATE

private val IrDeclarationWithVisibility.jvmVisibilityRank: Int
    get() = when (val flag = getVisibilityAccessFlag()) {
        Opcodes.ACC_PRIVATE -> 0
        AsmUtil.NO_FLAG_PACKAGE_PRIVATE -> 1
        Opcodes.ACC_PROTECTED -> 2
        Opcodes.ACC_PUBLIC -> 3
        else -> error("Unexpected visibility access flag: $flag")
    }

private fun IrSimpleFunction.computeJvmSignature(classCodegen: ClassCodegen): Method {
    if (isInCurrentModule()) return classCodegen.methodSignatureMapper.mapFakeOverrideSignatureSkipGeneric(this).asmMethod

    /*
     * Functions from other modules must be "manually lowered", since they did not go through the JvmLoweringPhases.
     * The two significant lowerings that affect JVM signature are:
     * 1. Inline class lowering, which mangles functions with inline class parameters.
     * 2. Suspend function lowering, which adds a continuation parameters to suspend functions.
     */

    val irFunction = classCodegen.context.inlineClassReplacements.getReplacementFunction(this) ?: this
    val method = classCodegen.methodSignatureMapper.mapFakeOverrideSignatureSkipGeneric(irFunction).asmMethod
    if (isSuspend) {
        // Add continuation parameter
        return Method(method.name, AsmTypes.OBJECT_TYPE, method.argumentTypes + CONTINUATION_ASM_TYPE)
    }
    return method
}
