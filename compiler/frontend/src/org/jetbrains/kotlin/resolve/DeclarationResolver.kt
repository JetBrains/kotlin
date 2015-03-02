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
import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import kotlin.Function1
import kotlin.KotlinPackage
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.TopLevelDescriptorProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.utils.*

import javax.inject.Inject
import java.util.HashSet

import org.jetbrains.kotlin.diagnostics.Errors.REDECLARATION

public class DeclarationResolver {
    private var annotationResolver: AnnotationResolver? = null
    private var trace: BindingTrace? = null


    Inject
    public fun setAnnotationResolver(annotationResolver: AnnotationResolver) {
        this.annotationResolver = annotationResolver
    }

    Inject
    public fun setTrace(trace: BindingTrace) {
        this.trace = trace
    }


    public fun resolveAnnotationsOnFiles(c: TopDownAnalysisContext, scopeProvider: FileScopeProvider) {
        val file2scope = c.getFiles().keysToMap<JetFile, JetScope>(object : Function1<JetFile, JetScope> {
            override fun invoke(file: JetFile): JetScope {
                return scopeProvider.getFileScope(file)
            }
        })

        resolveAnnotationsOnFiles(file2scope)
    }

    private fun resolveAnnotationsOnFiles(file2scope: Map<JetFile, out JetScope>) {
        for (entry in file2scope.entrySet()) {
            val file = entry.getKey()
            val fileScope = entry.getValue()
            annotationResolver!!.resolveAnnotationsWithArguments(fileScope, file.getAnnotationEntries(), trace)
            annotationResolver!!.resolveAnnotationsWithArguments(fileScope, file.getDanglingAnnotations(), trace)
        }
    }

    public fun checkRedeclarationsInInnerClassNames(c: TopDownAnalysisContext) {
        for (classDescriptor in c.getDeclaredClasses().values()) {
            if (classDescriptor.getKind() == ClassKind.CLASS_OBJECT) {
                // Class objects should be considered during analysing redeclarations in classes
                continue
            }

            var allDescriptors: MutableCollection<DeclarationDescriptor> = classDescriptor.getScopeForMemberLookup().getOwnDeclaredDescriptors()

            val classObj = classDescriptor.getDefaultObjectDescriptor()
            if (classObj != null) {
                val classObjDescriptors = classObj.getScopeForMemberLookup().getOwnDeclaredDescriptors()
                if (!classObjDescriptors.isEmpty()) {
                    allDescriptors = Lists.newArrayList<DeclarationDescriptor>(allDescriptors)
                    allDescriptors.addAll(classObjDescriptors)
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
            val descriptors = descriptorMap.get(name)
            if (descriptors.size() > 1) {
                // We mustn't compare PropertyDescriptor with PropertyDescriptor because we do this at OverloadResolver
                for (descriptor in descriptors) {
                    if (descriptor is ClassDescriptor) {
                        for (descriptor2 in descriptors) {
                            if (descriptor == descriptor2) {
                                continue
                            }

                            redeclarations.add(Pair.create<PsiElement, Name>(DescriptorToSourceUtils.classDescriptorToDeclaration(descriptor as ClassDescriptor), descriptor.getName()))
                            if (descriptor2 is PropertyDescriptor) {
                                redeclarations.add(Pair.create<PsiElement, Name>(DescriptorToSourceUtils.descriptorToDeclaration(descriptor2), descriptor2.getName()))
                            }
                        }
                    }
                }
            }
        }
        for (redeclaration in redeclarations) {
            trace!!.report(REDECLARATION.on(redeclaration.getFirst(), redeclaration.getSecond().asString()))
        }
    }

    public fun checkRedeclarationsInPackages(topLevelDescriptorProvider: TopLevelDescriptorProvider, topLevelFqNames: Multimap<FqName, JetElement>) {
        for (entry in topLevelFqNames.asMap().entrySet()) {
            val fqName = entry.getKey()
            val declarationsOrPackageDirectives = entry.getValue()

            if (fqName.isRoot()) continue

            val descriptors = getTopLevelDescriptorsByFqName(topLevelDescriptorProvider, fqName)

            if (descriptors.size() > 1) {
                for (declarationOrPackageDirective in declarationsOrPackageDirectives) {
                    val reportAt = if (declarationOrPackageDirective is JetNamedDeclaration)
                        declarationOrPackageDirective
                    else
                        (declarationOrPackageDirective as JetPackageDirective).getNameIdentifier()
                    trace!!.report(Errors.REDECLARATION.on(reportAt, fqName.shortName().asString()))
                }
            }
        }
    }

    class object {

        public fun getConstructorOfDataClass(classDescriptor: ClassDescriptor): ConstructorDescriptor {
            val constructors = classDescriptor.getConstructors()
            assert(constructors.size() == 1) { "Data class must have only one constructor: " + classDescriptor.getConstructors() }
            return constructors.iterator().next()
        }

        private fun getTopLevelDescriptorsByFqName(topLevelDescriptorProvider: TopLevelDescriptorProvider, fqName: FqName): Set<DeclarationDescriptor> {
            val parentFqName = fqName.parent()

            val descriptors = HashSet<DeclarationDescriptor>()

            val parentFragment = topLevelDescriptorProvider.getPackageFragment(parentFqName)
            if (parentFragment != null) {
                // Filter out extension properties
                descriptors.addAll(KotlinPackage.filter<VariableDescriptor>(parentFragment.getMemberScope().getProperties(fqName.shortName()), object : Function1<VariableDescriptor, Boolean> {
                    override fun invoke(descriptor: VariableDescriptor): Boolean? {
                        return descriptor.getExtensionReceiverParameter() == null
                    }
                }))
            }

            ContainerUtil.addIfNotNull<DeclarationDescriptor>(descriptors, topLevelDescriptorProvider.getPackageFragment(fqName))

            descriptors.addAll(topLevelDescriptorProvider.getTopLevelClassDescriptors(fqName))
            return descriptors
        }
    }


}
