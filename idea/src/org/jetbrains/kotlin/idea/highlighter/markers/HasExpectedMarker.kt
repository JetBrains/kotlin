/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter.markers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.facet.implementedDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun ModuleDescriptor.hasDeclarationOf(descriptor: MemberDescriptor) = declarationOf(descriptor) != null

private fun ModuleDescriptor.declarationOf(descriptor: MemberDescriptor): DeclarationDescriptor? =
        with(ExpectedActualResolver) {
            descriptor.findCompatibleExpectedForActual(this@declarationOf).firstOrNull()
        }

fun getExpectedDeclarationTooltip(declaration: KtDeclaration?): String? {
    val descriptor = declaration?.toDescriptor() as? MemberDescriptor ?: return null
    val platformModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val commonModuleDescriptor = platformModuleDescriptor.implementedDescriptor ?: return null
    if (!commonModuleDescriptor.hasDeclarationOf(descriptor)) return null

    return "Has declaration in common module"
}

fun navigateToExpectedDeclaration(declaration: KtDeclaration?) {
    declaration?.expectedDeclarationIfAny()?.navigate(false)
}

internal fun MemberDescriptor.expectedDescriptor() = module.implementedDescriptor?.declarationOf(this)

internal fun KtDeclaration.expectedDeclarationIfAny(): KtDeclaration? {
    val expectedDescriptor = (toDescriptor() as? MemberDescriptor)?.expectedDescriptor() ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(expectedDescriptor) as? KtDeclaration
}

internal fun KtDeclaration.isExpectedOrExpectedClassMember(): Boolean {
    if (hasExpectModifier()) return true
    if (this is KtClassOrObject) return this.isExpected()

    return containingClassOrObject?.isExpected() == true
}

internal fun KtClassOrObject.isExpected(): Boolean {
    return this.hasExpectModifier() ||
           this.descriptor.safeAs<ClassDescriptor>()?.isExpect == true
}

internal fun DeclarationDescriptor.liftToExpected(): DeclarationDescriptor? {
    if (this is MemberDescriptor) {
        return when {
            isExpect -> this
            isActual -> expectedDescriptor()
            else -> null
        }
    }

    if (this is ValueParameterDescriptor) {
        val containingExpectedDescriptor = containingDeclaration.liftToExpected() as? CallableDescriptor ?: return null
        return containingExpectedDescriptor.valueParameters.getOrNull(index)
    }

    return null
}

internal fun KtDeclaration.liftToExpected(): KtDeclaration? {
    val descriptor = resolveToDescriptorIfAny()
    val expectedDescriptor = descriptor?.liftToExpected() ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(expectedDescriptor) as? KtDeclaration
}