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

package org.jetbrains.kotlin.resolve

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.REDECLARATION
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.TopLevelDescriptorProvider
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

class DeclarationResolver(
        private val annotationResolver: AnnotationResolver,
        private val trace: BindingTrace
) {

    fun resolveAnnotationsOnFiles(c: TopDownAnalysisContext, scopeProvider: FileScopeProvider) {
        val filesToScope = c.files.keysToMap { scopeProvider.getFileResolutionScope(it) }
        for ((file, fileScope) in filesToScope) {
            annotationResolver.resolveAnnotationsWithArguments(fileScope, file.annotationEntries, trace)
            annotationResolver.resolveAnnotationsWithArguments(fileScope, file.danglingAnnotations, trace)
        }
    }

    fun checkRedeclarations(c: TopDownAnalysisContext) {
        for (classDescriptor in c.declaredClasses.values) {
            val descriptorMap = HashMultimap.create<Name, DeclarationDescriptor>()
            for (desc in classDescriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
                if (desc is ClassDescriptor || desc is PropertyDescriptor) {
                    descriptorMap.put(desc.name, desc)
                }
            }

            reportRedeclarations(descriptorMap)
        }
    }

    private fun reportRedeclarations(descriptorMap: Multimap<Name, DeclarationDescriptor>) {
        val redeclarations = Sets.newHashSet<Pair<PsiElement, Name>>()
        for (name in descriptorMap.keySet()) {
            val descriptors = descriptorMap[name]
            if (descriptors.size <= 1) {
                continue
            }
            // We mustn't compare PropertyDescriptor with PropertyDescriptor because we do this at OverloadResolver
            for (descriptor in descriptors) {
                if (descriptor is ClassDescriptor) {
                    for (descriptor2 in descriptors) {
                        if (descriptor === descriptor2) {
                            continue
                        }

                        redeclarations.add(Pair(DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)!!, descriptor.getName()))
                        if (descriptor2 is PropertyDescriptor) {
                            redeclarations.add(Pair(DescriptorToSourceUtils.descriptorToDeclaration(descriptor2)!!, descriptor2.getName()))
                        }
                    }
                }
            }
        }
        for ((first, second) in redeclarations) {
            trace.report(REDECLARATION.on(first, second.asString()))
        }
    }

    fun checkRedeclarationsInPackages(topLevelDescriptorProvider: TopLevelDescriptorProvider, topLevelFqNames: Multimap<FqName, KtElement>) {
        for ((fqName, declarationsOrPackageDirectives) in topLevelFqNames.asMap()) {
            if (fqName.isRoot) continue

            val descriptors = getTopLevelDescriptorsByFqName(topLevelDescriptorProvider, fqName, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)

            if (descriptors.size > 1) {
                for (declarationOrPackageDirective in declarationsOrPackageDirectives) {
                    val reportAt =
                            if (declarationOrPackageDirective is KtPackageDirective) declarationOrPackageDirective.getNameIdentifier()
                            else declarationOrPackageDirective
                    trace.report(Errors.REDECLARATION.on(reportAt!!, fqName.shortName().asString()))
                }
            }
        }
    }

    private fun getTopLevelDescriptorsByFqName(topLevelDescriptorProvider: TopLevelDescriptorProvider, fqName: FqName, location: LookupLocation): Set<DeclarationDescriptor> {
        val descriptors = HashSet<DeclarationDescriptor>()

        descriptors.addIfNotNull(topLevelDescriptorProvider.getPackageFragment(fqName))
        descriptors.addAll(topLevelDescriptorProvider.getTopLevelClassDescriptors(fqName, location))
        return descriptors
    }
}
