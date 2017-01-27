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
import com.sun.jdi.Location
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.findTopMostOverriddenDescriptors

class KotlinBasicStepMethodFilter(
        val descriptor: CallableDescriptor,
        val myCallingExpressionLines: Range<Int>
) : NamedMethodFilter {
    private val myTargetMethodName = when (descriptor) {
        is ClassDescriptor, is ConstructorDescriptor -> "<init>"
        is PropertyAccessorDescriptor -> JvmAbi.getterName(descriptor.correspondingProperty.name.asString())
        else -> descriptor.name.asString()
    }

    override fun getCallingExpressionLines() = myCallingExpressionLines

    override fun getMethodName() = myTargetMethodName

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        val method = location.method()
        if (myTargetMethodName != method.name()) return false

        val positionManager = process.positionManager ?: return false

        val descriptor = runReadAction {
            val elementAt = positionManager.getSourcePosition(location)?.elementAt

            val declaration = elementAt?.getParentOfTypesAndPredicate(false, KtDeclaration::class.java) {
                it !is KtProperty || !it.isLocal
            }

            if (declaration is KtClass && method.name() == "<init>") {
                (declaration.resolveToDescriptor() as? ClassDescriptor)?.unsubstitutedPrimaryConstructor
            } else {
                declaration?.resolveToDescriptor()
            }
        } ?: return true

        fun compareDescriptors(d1: DeclarationDescriptor, d2: DeclarationDescriptor): Boolean {
            return d1 == d2 || d1.original == d2.original
        }

        if (compareDescriptors(descriptor, this.descriptor)) return true

        if (descriptor is CallableDescriptor) {
           return descriptor.findTopMostOverriddenDescriptors().any { compareDescriptors(it, this.descriptor) } ||
                  this.descriptor.findTopMostOverriddenDescriptors().any { compareDescriptors(it, descriptor)}
        }

        return false
    }


}