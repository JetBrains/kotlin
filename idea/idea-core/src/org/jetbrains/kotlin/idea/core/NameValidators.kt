/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.getAllAccessibleFunctions
import org.jetbrains.kotlin.idea.util.getAllAccessibleVariables
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class CollectingNameValidator @JvmOverloads constructor(
    existingNames: Collection<String> = Collections.emptySet(),
    private val filter: (String) -> Boolean = { true }
) : (String) -> Boolean {
    private val existingNames = HashSet(existingNames)

    override fun invoke(name: String): Boolean {
        if (name !in existingNames && filter(name)) {
            existingNames.add(name)
            return true
        }
        return false
    }

    fun addName(name: String) {
        existingNames.add(name)
    }
}

class NewDeclarationNameValidator(
    private val visibleDeclarationsContext: KtElement?,
    private val checkDeclarationsIn: Sequence<PsiElement>,
    private val target: Target,
    private val excludedDeclarations: List<KtDeclaration> = emptyList()
) : (String) -> Boolean {
    constructor(
        container: PsiElement,
        anchor: PsiElement?,
        target: Target,
        excludedDeclarations: List<KtDeclaration> = emptyList()
    ) : this(
        (anchor ?: container).parentsWithSelf.firstIsInstanceOrNull<KtElement>(),
        anchor?.siblings() ?: container.allChildren,
        target,
        excludedDeclarations
    )

    enum class Target {
        VARIABLES,
        FUNCTIONS_AND_CLASSES
    }

    override fun invoke(name: String): Boolean {
        val identifier = Name.identifier(name)

        if (visibleDeclarationsContext != null) {
            val bindingContext = visibleDeclarationsContext.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION)
            val resolutionScope =
                visibleDeclarationsContext.getResolutionScope(bindingContext, visibleDeclarationsContext.getResolutionFacade())
            if (resolutionScope.hasConflict(identifier)) return false
        }

        return checkDeclarationsIn.none {
            it.findDescendantOfType<KtNamedDeclaration> { it.isConflicting(identifier) } != null
        }
    }

    private fun isExcluded(it: DeclarationDescriptorWithSource) = ErrorUtils.isError(it) || it.source.getPsi() in excludedDeclarations

    private fun LexicalScope.hasConflict(name: Name): Boolean {
        fun DeclarationDescriptor.isVisible(): Boolean {
            return when (this) {
                is DeclarationDescriptorWithVisibility -> isVisible(ownerDescriptor)
                else -> true
            }
        }

        return when (target) {
            Target.VARIABLES ->
                getAllAccessibleVariables(name).any { !it.isExtension && it.isVisible() && !isExcluded(it) }
            Target.FUNCTIONS_AND_CLASSES ->
                getAllAccessibleFunctions(name).any { !it.isExtension && it.isVisible() && !isExcluded(it) } ||
                        findClassifier(name, NoLookupLocation.FROM_IDE)?.let { it.isVisible() && !isExcluded(it) } ?: false
        }
    }

    private fun KtNamedDeclaration.isConflicting(name: Name): Boolean {
        if (this in excludedDeclarations) return false
        if (nameAsName != name) return false
        if (this is KtCallableDeclaration && receiverTypeReference != null) return false
        return when (target) {
            Target.VARIABLES -> this is KtVariableDeclaration || this is KtParameter
            Target.FUNCTIONS_AND_CLASSES -> this is KtNamedFunction || this is KtClassOrObject || this is KtTypeAlias
        }
    }
}
