/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.references.fe10.base.KtFe10ReferenceResolutionHelper
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.resolve.BindingContext

class KtFe10DestructuringDeclarationEntry(
    element: KtDestructuringDeclarationEntry
) : KtDestructuringDeclarationReference(element), KtFe10Reference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return listOfNotNull(
            context[BindingContext.VARIABLE, element],
            context[BindingContext.COMPONENT_RESOLVED_CALL, element]?.candidateDescriptor
        )
    }

    override fun getRangeInElement() = TextRange(0, element.textLength)

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KtFe10Reference>.isReferenceToImportAlias(alias)
    }

    override fun canRename(): Boolean {
        val bindingContext = KtFe10ReferenceResolutionHelper.getInstance().partialAnalyze(element) //TODO: should it use full body resolve?
        return resolveToDescriptors(bindingContext).all {
            it is CallableMemberDescriptor && it.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
        }
    }
}
