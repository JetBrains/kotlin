/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.references.ReferenceAccess
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.utils.addIfNotNull

@OptIn(KtImplementationDetail::class)
internal class Fe10SyntheticPropertyAccessorReference(
    expression: KtNameReferenceExpression,
    getter: Boolean
) : SyntheticPropertyAccessorReference(expression, getter), KtFe10Reference {

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val descriptors = expression.getReferenceTargets(context)
        if (descriptors.none { it is SyntheticJavaPropertyDescriptor }) return emptyList()

        val result = SmartList<FunctionDescriptor>()
        for (descriptor in descriptors) {
            if (descriptor is SyntheticJavaPropertyDescriptor) {
                if (getter) {
                    result.add(descriptor.getMethod)
                } else {
                    if (descriptor.setMethod == null) result.addIfNotNull(descriptor.getMethod)
                    else result.addIfNotNull(descriptor.setMethod)
                }
            }
        }
        return result
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KtFe10Reference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtNameReferenceExpression> {
        override val elementClass: Class<KtNameReferenceExpression>
            get() = KtNameReferenceExpression::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtNameReferenceExpression>
            get() = provider@{ nameReferenceExpression ->
                if (nameReferenceExpression.getReferencedNameElementType() != KtTokens.IDENTIFIER) {
                    return@provider emptyList()
                }
                if (nameReferenceExpression.parents.any { it is KtImportDirective || it is KtPackageDirective || it is KtUserType }) {
                    return@provider emptyList()
                }

                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ -> listOf(Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = true))
                    ReferenceAccess.WRITE -> listOf(Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = false))
                    ReferenceAccess.READ_WRITE -> listOf(
                        Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = true),
                        Fe10SyntheticPropertyAccessorReference(nameReferenceExpression, getter = false),
                    )
                }
            }
    }
}
