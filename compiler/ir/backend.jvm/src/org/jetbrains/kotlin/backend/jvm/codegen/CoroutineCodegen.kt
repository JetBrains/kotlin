/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.org.objectweb.asm.MethodVisitor

fun wrapWithCoroutineTransformer(
    irFunction: IrFunction,
    classCodegen: ClassCodegen,
    methodVisitor: MethodVisitor,
    access: Int,
    signature: JvmMethodGenericSignature
): MethodVisitor {
    return methodVisitor
//    assert(irFunction.isSuspend)
//    val state = classCodegen.state
//    val languageVersionSettings = state.languageVersionSettings
//    assert(languageVersionSettings.isReleaseCoroutines()) { "Experimental coroutines are unsupported in JVM_IR backend" }
//    val continuationClass = createContinuationClassForNamedFunction()
//    CoroutineTransformerMethodVisitor(
//        methodVisitor, access, signature.asmMethod.name, signature.asmMethod.descriptor, null, null,
//        obtainClassBuilderForCoroutineState = { TODO() },
//        element = irFunction.symbol.descriptor.psiElement as KtElement,
//        diagnostics = state.diagnostics,
//        languageVersionSettings = languageVersionSettings,
//        shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
//        containingClassInternalName = classCodegen.visitor.thisName,
//        isForNamedFunction = true,
//        needDispatchReceiver = true,
//        internalNameForDispatchReceiver = classCodegen.visitor.thisName
//    )
}
