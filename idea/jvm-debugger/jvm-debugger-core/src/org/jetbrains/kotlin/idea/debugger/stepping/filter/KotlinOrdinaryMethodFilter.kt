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

package org.jetbrains.kotlin.idea.debugger.stepping.filter

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.NamedMethodFilter
import com.intellij.util.Range
import com.sun.jdi.Location
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.debugger.stepping.smartStepInto.KotlinMethodSmartStepTarget
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.DescriptorUtils

class KotlinOrdinaryMethodFilter(target: KotlinMethodSmartStepTarget) : NamedMethodFilter {
    private val declarationPtr = target.declaration?.createSmartPointer()
    private val isInvoke = target.isInvoke
    private val targetMethodName = target.targetMethodName
    private val myCallingExpressionLines: Range<Int>? = target.callingExpressionLines

    init {
        assert(declarationPtr != null || isInvoke)
    }

    override fun getCallingExpressionLines() = myCallingExpressionLines

    override fun getMethodName() = targetMethodName

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        val method = location.method()
        if (targetMethodName != method.name()) return false

        val positionManager = process.positionManager

        val (currentDescriptor, currentDeclaration) = runReadAction {
            val elementAt = positionManager.getSourcePosition(location)?.elementAt

            val declaration = elementAt?.getParentOfTypesAndPredicate(false, KtDeclaration::class.java) {
                it !is KtProperty || !it.isLocal
            }

            if (declaration is KtClass && method.name() == "<init>") {
                declaration.resolveToDescriptorIfAny()?.unsubstitutedPrimaryConstructor to declaration
            } else {
                declaration?.resolveToDescriptorIfAny() to declaration
            }
        }

        if (currentDescriptor == null || currentDeclaration == null) {
            return false
        }

        @Suppress("FoldInitializerAndIfToElvis")
        if (currentDescriptor !is CallableMemberDescriptor) return false
        if (currentDescriptor.kind != DECLARATION) return false

        if (isInvoke) {
            // There can be only one 'invoke' target at the moment so consider position as expected.
            // Descriptors can be not-equal, say when parameter has type `(T) -> T` and lambda is `Int.() -> Int`.
            return true
        }

        // Element is lost. But we know that name is matches, so stop.
        val declaration = runReadAction { declarationPtr?.element } ?: return true

        val psiManager = currentDeclaration.manager
        if (psiManager.areElementsEquivalent(currentDeclaration, declaration)) {
            return true
        }

        return DescriptorUtils.getAllOverriddenDescriptors(currentDescriptor).any { baseOfCurrent ->
            val currentBaseDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(currentDeclaration.project, baseOfCurrent)
            psiManager.areElementsEquivalent(declaration, currentBaseDeclaration)
        }
    }
}