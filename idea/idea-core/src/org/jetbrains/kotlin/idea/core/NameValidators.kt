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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

public class CollectingNameValidator @jvmOverloads constructor(
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
        private val visibleDeclarationsContext: JetElement?,
        private val checkDeclarationsIn: Sequence<PsiElement>,
        private val target: NewDeclarationNameValidator.Target
) : (String) -> Boolean {

    public constructor(container: PsiElement, anchor: PsiElement?, target: NewDeclarationNameValidator.Target)
        : this(
            (anchor ?: container).parentsWithSelf.firstIsInstanceOrNull<JetElement>(),
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
            if (resolutionScope.hasConflict(identifier)) return false
        }

        return checkDeclarationsIn.none {
            it.findDescendantOfType<JetNamedDeclaration> { it.isConflicting(identifier) } != null
        }
    }

    private fun JetScope.hasConflict(name: Name): Boolean {
        return when(target) {
            Target.VARIABLES -> getProperties(name).any { !it.isExtension } || getLocalVariable(name) != null
            Target.FUNCTIONS_AND_CLASSES -> getFunctions(name).any { !it.isExtension } || getClassifier(name) != null
        }
    }

    private fun JetNamedDeclaration.isConflicting(name: Name): Boolean {
        if (getNameAsName() != name) return false
        if (this is JetCallableDeclaration && getReceiverTypeReference() != null) return false
        return when(target) {
            Target.VARIABLES -> this is JetVariableDeclaration
            Target.FUNCTIONS_AND_CLASSES -> this is JetNamedFunction || this is JetClassOrObject
        }
    }
}
