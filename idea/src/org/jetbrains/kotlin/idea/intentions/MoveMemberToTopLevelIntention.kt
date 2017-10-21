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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeCheckerImpl
import org.jetbrains.kotlin.types.typeUtil.equalTypesOrNulls
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class MoveMemberToTopLevelIntention : MoveMemberOutOfObjectIntention("Move to top level") {
    override fun addConflicts(element: KtNamedDeclaration, conflicts: MultiMap<PsiElement, String>) {
        val packageViewDescriptor = element.findModuleDescriptor().getPackage(element.containingKtFile.packageFqName)
        val packageDescriptor = packageViewDescriptor.fragments.firstIsInstance<LazyPackageDescriptor>()
        val memberScope = packageDescriptor.getMemberScope()
        val packageName = packageViewDescriptor.fqName.asString().takeIf { it.isNotBlank() } ?: "default"

        when (element) {
            is KtProperty -> if (memberScope.getVariableNames().any { element.name == it.identifier }) {
                conflicts.putValue(element, "Package '$packageName' already contains ${RefactoringUIUtil.getDescription(element, false)}")
            }

            is KtFunction -> {
                val name = element.name ?: return
                if (memberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).filter {
                    descriptorsEqual(element.descriptor, it)
                }.isNotEmpty()) {
                    conflicts.putValue(element, "Package '$packageName' already contains ${RefactoringUIUtil.getDescription(element, false)}")
                }
            }
        }

    }

    private fun descriptorsEqual(descriptor1: DeclarationDescriptor?, descriptor2: DeclarationDescriptor?): Boolean {
        // This method comes from org.jetbrains.kotlin.util.DescriptorUtilsKt.descriptorsEqualWithSubstitution
        // However, the method checks `descriptor1.original == descriptor2.original` which always false in this intention
        if (descriptor1 == descriptor2) return true
        if (descriptor1 == null || descriptor2 == null) return false
        if (descriptor1 !is CallableDescriptor) return true
        descriptor2 as CallableDescriptor

        val typeChecker = KotlinTypeCheckerImpl.withAxioms(KotlinTypeChecker.TypeConstructorEquality { a, b ->
            val typeParam1 = a.declarationDescriptor as? TypeParameterDescriptor
            val typeParam2 = b.declarationDescriptor as? TypeParameterDescriptor
            if (typeParam1 != null
                && typeParam2 != null
                && typeParam1.containingDeclaration == descriptor1
                && typeParam2.containingDeclaration == descriptor2) {
                return@TypeConstructorEquality typeParam1.index == typeParam2.index
            }

            a == b
        })

        if (!typeChecker.equalTypesOrNulls(descriptor1.returnType, descriptor2.returnType)) return false

        val parameters1 = descriptor1.valueParameters
        val parameters2 = descriptor2.valueParameters
        if (parameters1.size != parameters2.size) return false
        for ((param1, param2) in parameters1.zip(parameters2)) {
            if (!typeChecker.equalTypes(param1.type, param2.type)) return false
        }
        return true
    }

    override fun getDestination(element: KtNamedDeclaration) = element.containingKtFile

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtNamedFunction && element !is KtProperty && element !is KtClassOrObject) return null
        element.containingClassOrObject as? KtObjectDeclaration ?: return null
        return element.nameIdentifier?.textRange
    }

}