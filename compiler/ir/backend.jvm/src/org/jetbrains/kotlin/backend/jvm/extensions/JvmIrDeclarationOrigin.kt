/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.extensions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.psi.PsiSourceManager
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isEffectivelyInlineOnly
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

class JvmIrDeclarationOrigin(
    originKind: JvmDeclarationOriginKind,
    element: PsiElement?,
    val declaration: IrDeclaration?,
) : JvmDeclarationOrigin(originKind, element, declaration?.toIrBasedDescriptor(), null) {
    override val originalSourceElement: Any?
        get() = (declaration as? IrAttributeContainer)?.attributeOwnerId
}

val IrDeclaration.descriptorOrigin: JvmIrDeclarationOrigin
    get() {
        val psiElement = findPsiElementForDeclarationOrigin()
        return when {
            origin == IrDeclarationOrigin.FILE_CLASS ->
                JvmIrDeclarationOrigin(JvmDeclarationOriginKind.PACKAGE_PART, psiElement, this)
            (this is IrSimpleFunction && isSuspend && isEffectivelyInlineOnly()) ||
                    origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE ||
                    origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE ->
                JvmIrDeclarationOrigin(JvmDeclarationOriginKind.INLINE_VERSION_OF_SUSPEND_FUN, psiElement, this)
            else -> JvmIrDeclarationOrigin(JvmDeclarationOriginKind.OTHER, psiElement, this)
        }
    }

private fun IrDeclaration.findPsiElementForDeclarationOrigin(): PsiElement? {
    // For synthetic $annotations methods for properties, use the PSI for the property or the constructor parameter.
    // It's used in KAPT stub generation to sort the properties correctly based on their source position (see KT-44130).
    if (this is IrFunction && name.asString().endsWith("\$annotations")) {
        val metadata = metadata as? DescriptorMetadataSource.Property
        if (metadata != null) {
            return metadata.descriptor.psiElement
        }
    }

    val element = PsiSourceManager.findPsiElement(this)

    // Offsets for accessors and field of delegated property in IR point to the 'by' keyword, so the closest PSI element is the
    // KtPropertyDelegate (`by ...` expression). However, old JVM backend passed the PSI element of the property instead.
    // This is important for example in case of KAPT stub generation in the "correct error types" mode, which tries to find the
    // PSI element for each declaration with unresolved types and tries to heuristically "resolve" those unresolved types to
    // generate them into the Java stub. In case of delegated property accessors, it should look for the property declaration,
    // since the type can only be provided there, and not in the `by ...` expression.
    return if (element is KtPropertyDelegate) element.parent else element
}
