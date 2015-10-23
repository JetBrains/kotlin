/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.getAllAccessibleFunctions
import org.jetbrains.kotlin.idea.util.getAllAccessibleVariables
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.utils.asKtScope
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.Collections
import java.util.HashSet

public class CollectingNameValidator @JvmOverloads constructor(
        existingNames: Collection<String> = Collections.emptySet(),
        private val filter: (String) -> Boolean = { true }
): (String) -> Boolean {
    private val existingNames = HashSet(existingNames)

    override fun invoke(name: String): Boolean {
        if (name !in existingNames && filter(name)) {
            existingNames.add(name)
            return true
        }
        return false
    }

    public fun addName(name: String) {
        existingNames.add(name)
    }
}

public class NewDeclarationNameValidator(
        private val visibleDeclarationsContext: KtElement?,
        private val checkDeclarationsIn: Sequence<PsiElement>,
        private val target: NewDeclarationNameValidator.Target
) : (String) -> Boolean {

    public constructor(container: PsiElement, anchor: PsiElement?, target: NewDeclarationNameValidator.Target)
        : this(
            (anchor ?: container).parentsWithSelf.firstIsInstanceOrNull<KtElement>(),
            anchor?.siblings() ?: container.allChildren,
            target)

    public enum class Target {
        VARIABLES,
        FUNCTIONS_AND_CLASSES
    }

    override fun invoke(name: String): Boolean {
        val identifier = Name.identifier(name)

        if (visibleDeclarationsContext != null) {
            val bindingContext = visibleDeclarationsContext.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION)
            val resolutionScope = visibleDeclarationsContext.getResolutionScope(bindingContext, visibleDeclarationsContext.getResolutionFacade())
            if (resolutionScope.asKtScope().hasConflict(identifier)) return false
        }

        return checkDeclarationsIn.none {
            it.findDescendantOfType<KtNamedDeclaration> { it.isConflicting(identifier) } != null
        }
    }

    private fun KtScope.hasConflict(name: Name): Boolean {
        val inDeclaration = getContainingDeclaration()

        fun DeclarationDescriptor.isVisible(): Boolean {
            return when (this) {
                is DeclarationDescriptorWithVisibility -> isVisible(inDeclaration)
                else -> true
            }
        }

        return when(target) {
            Target.VARIABLES ->
                getAllAccessibleVariables(name).any { !it.isExtension && it.isVisible() }
            Target.FUNCTIONS_AND_CLASSES ->
                getAllAccessibleFunctions(name).any { !it.isExtension && it.isVisible() } ||
                getClassifier(name, NoLookupLocation.FROM_IDE)?.let { it.isVisible() } ?: false
        }
    }

    private fun KtNamedDeclaration.isConflicting(name: Name): Boolean {
        if (getNameAsName() != name) return false
        if (this is KtCallableDeclaration && getReceiverTypeReference() != null) return false
        return when(target) {
            Target.VARIABLES -> this is KtVariableDeclaration
            Target.FUNCTIONS_AND_CLASSES -> this is KtNamedFunction || this is KtClassOrObject
        }
    }
}
