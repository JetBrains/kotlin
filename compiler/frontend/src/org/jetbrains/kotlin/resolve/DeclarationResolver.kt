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
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.REDECLARATION
import org.jetbrains.kotlin.diagnostics.reportOnDeclaration
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
                if (desc is ClassifierDescriptor || desc is PropertyDescriptor) {
                    descriptorMap.put(desc.name, desc)
                }
            }

            reportRedeclarationsWithClassifiers(descriptorMap)
        }
    }

    private fun reportRedeclarationsWithClassifiers(descriptorMap: Multimap<Name, DeclarationDescriptor>) {
        for (name in descriptorMap.keySet()) {
            val descriptors = descriptorMap[name]
            if (descriptors.size > 1 && descriptors.any { it is ClassifierDescriptor }) {
                for (descriptor in descriptors) {
                    reportOnDeclaration(trace, descriptor) { REDECLARATION.on(it, descriptors) }
                }
            }
        }
    }

    fun checkRedeclarationsInPackages(topLevelDescriptorProvider: TopLevelDescriptorProvider, topLevelFqNames: Multimap<FqName, KtElement>) {
        for ((fqName, declarationsOrPackageDirectives) in topLevelFqNames.asMap()) {
            if (fqName.isRoot) continue

            // TODO: report error on expected class and actual val, or vice versa
            val (expected, actual) =
                    getTopLevelDescriptorsByFqName(topLevelDescriptorProvider, fqName, NoLookupLocation.WHEN_CHECK_DECLARATION_CONFLICTS)
                    .partition { it is MemberDescriptor && it.isExpect }

            for (descriptors in listOf(expected, actual)) {
                if (descriptors.size > 1) {
                    for (directive in declarationsOrPackageDirectives) {
                        val reportAt = (directive as? KtPackageDirective)?.nameIdentifier ?: directive
                        trace.report(Errors.PACKAGE_OR_CLASSIFIER_REDECLARATION.on(reportAt, fqName.shortName().asString()))
                    }
                }
            }
        }
    }

    private fun getTopLevelDescriptorsByFqName(topLevelDescriptorProvider: TopLevelDescriptorProvider, fqName: FqName, location: LookupLocation): Set<DeclarationDescriptor> {
        val descriptors = HashSet<DeclarationDescriptor>()

        descriptors.addIfNotNull(topLevelDescriptorProvider.getPackageFragment(fqName))
        descriptors.addAll(topLevelDescriptorProvider.getTopLevelClassifierDescriptors(fqName, location))
        return descriptors
    }
}
