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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.NamedMethodFilter
import com.intellij.util.Range
import com.intellij.util.SofterReference
import com.sun.jdi.Location
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.getDirectlyOverriddenDeclarations
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.DescriptorUtils

class KotlinBasicStepMethodFilter(
        targetDescriptor: CallableMemberDescriptor,
        private val myCallingExpressionLines: Range<Int>
) : NamedMethodFilter {
    private val myTargetMethodName: String = when (targetDescriptor) {
        is ClassDescriptor, is ConstructorDescriptor -> "<init>"
        is PropertyAccessorDescriptor -> JvmAbi.getterName(targetDescriptor.correspondingProperty.name.asString())
        else -> targetDescriptor.name.asString()
    }

    private val _targetDescriptor = SofterReference(targetDescriptor)

    override fun getCallingExpressionLines() = myCallingExpressionLines

    override fun getMethodName() = myTargetMethodName

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        val targetDescriptor = _targetDescriptor.get() ?: return true

        val method = location.method()
        if (myTargetMethodName != method.name()) return false

        val positionManager = process.positionManager

        val currentDescriptor = runReadAction {
            val elementAt = positionManager.getSourcePosition(location)?.elementAt

            val declaration = elementAt?.getParentOfTypesAndPredicate(false, KtDeclaration::class.java) {
                it !is KtProperty || !it.isLocal
            }

            if (declaration is KtClass && method.name() == "<init>") {
                (declaration.resolveToDescriptor() as? ClassDescriptor)?.unsubstitutedPrimaryConstructor
            } else {
                declaration?.resolveToDescriptor()
            }
        } ?: return false // TODO: Check that we can always find a descriptor (libraries with sources, libraries without sources)

        @Suppress("FoldInitializerAndIfToElvis")
        if (currentDescriptor !is CallableMemberDescriptor) return false
        if (currentDescriptor.kind != DECLARATION) return false

        if (targetDescriptor is FunctionInvokeDescriptor) {
            // There can be only one 'invoke' target at the moment so consider position as expected.
            // Descriptors can be not-equal, say when parameter has type `(T) -> T` and lambda is `Int.() -> Int`.
            return true
        }

        if (compareDescriptors(currentDescriptor, targetDescriptor)) return true

        // We should stop if current descriptor overrides the target one or some base descriptor of target
        // (if target descriptor is delegation or fake override)

        val baseDescriptors = when (targetDescriptor.kind) {
            DELEGATION, FAKE_OVERRIDE ->
                targetDescriptor.getDirectlyOverriddenDeclarations()
            DECLARATION, SYNTHESIZED ->
                listOf(targetDescriptor)
        }

        if (baseDescriptors.any { baseOfTarget -> compareDescriptors(baseOfTarget, currentDescriptor) }) {
            return true
        }

        return DescriptorUtils.getAllOverriddenDescriptors(currentDescriptor).any { baseOfCurrent ->
            baseDescriptors.any { baseOfTarget -> compareDescriptors(baseOfCurrent, baseOfTarget) }
        }
    }
}

private fun compareDescriptors(d1: DeclarationDescriptor, d2: DeclarationDescriptor): Boolean {
    return d1 == d2 || d1.original == d2.original
}