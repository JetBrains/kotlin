/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.classMembers.AbstractMemberInfoStorage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverloadChecker
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import java.util.*

class KotlinMemberInfoStorage(
        classOrObject: KtClassOrObject,
        filter: (KtNamedDeclaration) -> Boolean = { true }
): AbstractMemberInfoStorage<KtNamedDeclaration, PsiNamedElement, KotlinMemberInfo>(classOrObject, filter) {
    override fun memberConflict(member1: KtNamedDeclaration, member: KtNamedDeclaration): Boolean {
        val descriptor1 = member1.resolveToDescriptor()
        val descriptor = member.resolveToDescriptor()
        if (descriptor1.name != descriptor.name) return false

        return when {
            descriptor1 is FunctionDescriptor && descriptor is FunctionDescriptor -> {
                val overloadUtil = member1.getResolutionFacade().frontendService<OverloadChecker>()
                !overloadUtil.isOverloadable(descriptor1, descriptor)
            }
            descriptor1 is PropertyDescriptor && descriptor is PropertyDescriptor ||
            descriptor1 is ClassDescriptor && descriptor is ClassDescriptor -> true
            else -> false
        }
    }

    override fun buildSubClassesMap(aClass: PsiNamedElement) {
        val classDescriptor = aClass.getClassDescriptorIfAny() ?: return
        val classType = classDescriptor.defaultType
        for (supertype in classType.immediateSupertypes()) {
            val superClass = supertype.constructor.declarationDescriptor?.source?.getPsi()
            if (superClass is KtClass || superClass is PsiClass) {
                getSubclasses(superClass as PsiNamedElement).add(aClass)
                buildSubClassesMap(superClass)
            }
        }
    }

    override fun isInheritor(baseClass: PsiNamedElement, aClass: PsiNamedElement): Boolean {
        val baseDescriptor = baseClass.getClassDescriptorIfAny() ?: return false
        val currentDescriptor = aClass.getClassDescriptorIfAny() ?: return false
        return DescriptorUtils.isSubclass(currentDescriptor, baseDescriptor)
    }

    override fun extractClassMembers(aClass: PsiNamedElement, temp: ArrayList<KotlinMemberInfo>) {
        if (aClass !is KtClassOrObject) return

        val context = aClass.analyze()
        aClass.declarations
                .filter { it is KtNamedDeclaration
                          && it !is KtConstructor<*>
                          && !(it is KtObjectDeclaration && it.isCompanion())
                          && myFilter.includeMember(it) }
                .mapTo(temp) { KotlinMemberInfo(it as KtNamedDeclaration) }
        if (aClass == myClass) {
            aClass.getSuperTypeListEntries()
                    .filterIsInstance<KtSuperTypeEntry>()
                    .map {
                        val type = context[BindingContext.TYPE, it.typeReference]
                        val classDescriptor = type?.constructor?.declarationDescriptor as? ClassDescriptor
                        classDescriptor?.source?.getPsi() as? KtClass
                    }
                    .filter { it != null && it.isInterface() }
                    .mapTo(temp) { KotlinMemberInfo(it!!, true) }
        }
    }
}