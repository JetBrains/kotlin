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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.TopLevelDescriptorProvider
import org.jetbrains.kotlin.utils.*

import javax.inject.Inject
import java.util.HashSet

import org.jetbrains.kotlin.diagnostics.Errors.REDECLARATION
import kotlin.properties.Delegates

public class DeclarationResolver {
    private var _annotationResolver: AnnotationResolver by Delegates.notNull()
    private var _trace: BindingTrace by Delegates.notNull()

    Inject
    public fun setAnnotationResolver(annotationResolver: AnnotationResolver) {
        this._annotationResolver = annotationResolver
    }

    Inject
    public fun setTrace(trace: BindingTrace) {
        this._trace = trace
    }

    public fun resolveAnnotationsOnFiles(c: TopDownAnalysisContext, scopeProvider: FileScopeProvider) {
        val filesToScope = c.getFiles().keysToMap { scopeProvider.getFileScope(it) }
        for ((file, fileScope) in filesToScope) {
            _annotationResolver.resolveAnnotationsWithArguments(fileScope, file.getAnnotationEntries(), _trace)
            _annotationResolver.resolveAnnotationsWithArguments(fileScope, file.getDanglingAnnotations(), _trace)
        }
    }

    public fun checkRedeclarationsInInnerClassNames(c: TopDownAnalysisContext) {
        for (classDescriptor in c.getDeclaredClasses().values()) {
            if (classDescriptor.getKind() == ClassKind.CLASS_OBJECT) {
                // Default objects should be considered during analysing redeclarations in classes
                continue
            }

            var allDescriptors = classDescriptor.getScopeForMemberLookup().getOwnDeclaredDescriptors()
            val defaultObject = classDescriptor.getDefaultObjectDescriptor()
            if (defaultObject != null) {
                val descriptorsFromDefaultObject = defaultObject.getScopeForMemberLookup().getOwnDeclaredDescriptors()
                if (descriptorsFromDefaultObject.isNotEmpty()) {
                    allDescriptors = allDescriptors + descriptorsFromDefaultObject
                }
            }

            val descriptorMap = HashMultimap.create<Name, DeclarationDescriptor>()
            for (desc in allDescriptors) {
                if (desc is ClassDescriptor || desc is PropertyDescriptor) {
                    descriptorMap.put(desc.getName(), desc)
                }
            }

            reportRedeclarations(descriptorMap)
        }
    }

    private fun reportRedeclarations(descriptorMap: Multimap<Name, DeclarationDescriptor>) {
        val redeclarations = Sets.newHashSet<Pair<PsiElement, Name>>()
        for (name in descriptorMap.keySet()) {
            val descriptors = descriptorMap[name]
            if (descriptors.size() <= 1) {
                continue
            }
            // We mustn't compare PropertyDescriptor with PropertyDescriptor because we do this at OverloadResolver
            for (descriptor in descriptors) {
                if (descriptor is ClassDescriptor) {
                    for (descriptor2 in descriptors) {
                        if (descriptor identityEquals descriptor2) {
                            continue
                        }

                        redeclarations.add(Pair(DescriptorToSourceUtils.classDescriptorToDeclaration(descriptor), descriptor.getName()))
                        if (descriptor2 is PropertyDescriptor) {
                            redeclarations.add(Pair(DescriptorToSourceUtils.descriptorToDeclaration(descriptor2), descriptor2.getName()))
                        }
                    }
                }
            }
        }
        for ((first, second) in redeclarations) {
            _trace.report(REDECLARATION.on(first, second.asString()))
        }
    }

    public fun checkRedeclarationsInPackages(topLevelDescriptorProvider: TopLevelDescriptorProvider, topLevelFqNames: Multimap<FqName, JetElement>) {
        for ((fqName, declarationsOrPackageDirectives) in topLevelFqNames.asMap()) {
            if (fqName.isRoot()) continue

            val descriptors = getTopLevelDescriptorsByFqName(topLevelDescriptorProvider, fqName)

            if (descriptors.size() > 1) {
                for (declarationOrPackageDirective in declarationsOrPackageDirectives) {
                    val reportAt =
                            if (declarationOrPackageDirective is JetPackageDirective) declarationOrPackageDirective.getNameIdentifier()
                            else declarationOrPackageDirective
                    _trace.report(Errors.REDECLARATION.on(reportAt, fqName.shortName().asString()))
                }
            }
        }
    }

    private fun getTopLevelDescriptorsByFqName(topLevelDescriptorProvider: TopLevelDescriptorProvider, fqName: FqName): Set<DeclarationDescriptor> {
        val parentFqName = fqName.parent()

        val descriptors = HashSet<DeclarationDescriptor>()

        val parentFragment = topLevelDescriptorProvider.getPackageFragment(parentFqName)
        if (parentFragment != null) {
            // Filter out extension properties
            descriptors.addAll(parentFragment.getMemberScope().getProperties(fqName.shortName()).filter {
                it.getExtensionReceiverParameter() == null
            })
        }

        descriptors.addIfNotNull(topLevelDescriptorProvider.getPackageFragment(fqName))
        descriptors.addAll(topLevelDescriptorProvider.getTopLevelClassDescriptors(fqName))
        return descriptors
    }
}
