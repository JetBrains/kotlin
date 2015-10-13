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

package org.jetbrains.kotlin.idea.core

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.stubs.StringStubIndexExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.receiverTypes
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.isAnnotatedAsHidden
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

public class KotlinIndicesHelper(
        private val resolutionFacade: ResolutionFacade,
        private val scope: GlobalSearchScope,
        visibilityFilter: (DeclarationDescriptor) -> Boolean,
        applyExcludeSettings: Boolean
) {

    private val moduleDescriptor = resolutionFacade.moduleDescriptor
    private val project = resolutionFacade.project

    private val descriptorFilter: (DeclarationDescriptor) -> Boolean = filter@ {
        if (it.isAnnotatedAsHidden()) return@filter false
        if (!visibilityFilter(it)) return@filter false
        if (applyExcludeSettings && isExcludedFromAutoImport(it)) return@filter false
        true
    }

    public fun getTopLevelCallablesByName(name: String): Collection<CallableDescriptor> {
        val declarations = HashSet<JetNamedDeclaration>()
        declarations.addTopLevelNonExtensionCallablesByName(JetFunctionShortNameIndex.getInstance(), name)
        declarations.addTopLevelNonExtensionCallablesByName(JetPropertyShortNameIndex.getInstance(), name)
        return declarations.flatMap {
            if (it.getContainingJetFile().isCompiled()) { //TODO: it's temporary while resolveToDescriptor does not work for compiled declarations
                resolutionFacade.resolveImportReference(moduleDescriptor, it.getFqName()!!).filterIsInstance<CallableDescriptor>()
            }
            else {
                (resolutionFacade.resolveToDescriptor(it) as? CallableDescriptor).singletonOrEmptyList()
            }
        }.filter { it.getExtensionReceiverParameter() == null && descriptorFilter(it) }
    }

    private fun MutableSet<JetNamedDeclaration>.addTopLevelNonExtensionCallablesByName(
            index: StringStubIndexExtension<out JetCallableDeclaration>,
            name: String
    ) {
        index.get(name, project, scope).filterTo(this) { it.getParent() is JetFile && it.getReceiverTypeReference() == null }
    }

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean): Collection<CallableDescriptor> {
        return (JetTopLevelFunctionFqnNameIndex.getInstance().getAllKeys(project).asSequence() +
                    JetTopLevelPropertyFqnNameIndex.getInstance().getAllKeys(project).asSequence())
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it).filter(descriptorFilter) }
    }

    public fun getCallableTopLevelExtensions(
            nameFilter: (String) -> Boolean,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            position: JetExpression,
            bindingContext: BindingContext
    ): Collection<CallableDescriptor> {
        val receiverTypes = callTypeAndReceiver.receiverTypes(bindingContext, position, moduleDescriptor, predictableSmartCastsOnly = false)
        if (receiverTypes == null || receiverTypes.isEmpty()) return emptyList()

        val receiverTypeNames = HashSet<String>()
        receiverTypes.forEach { receiverTypeNames.addTypeNames(it) }

        val index = JetTopLevelExtensionsByReceiverTypeIndex.INSTANCE

        val declarations = index.getAllKeys(project)
                .asSequence()
                .filter {
                    JetTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames
                    && nameFilter(JetTopLevelExtensionsByReceiverTypeIndex.callableNameFromKey(it))
                }
                .flatMap { index.get(it, project, scope).asSequence() }

        return findSuitableExtensions(declarations, receiverTypes, callTypeAndReceiver.callType)
    }

    private fun MutableCollection<String>.addTypeNames(type: JetType) {
        val constructor = type.getConstructor()
        addIfNotNull(constructor.getDeclarationDescriptor()?.getName()?.asString())
        constructor.getSupertypes().forEach { addTypeNames(it) }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(
            declarations: Sequence<JetCallableDeclaration>,
            receiverTypes: Collection<JetType>,
            callType: CallType<*>
    ): Collection<CallableDescriptor> {
        val result = LinkedHashSet<CallableDescriptor>()

        fun processDescriptor(descriptor: CallableDescriptor) {
            if (descriptorFilter(descriptor)) {
                result.addAll(descriptor.substituteExtensionIfCallable(receiverTypes, callType))
            }
        }

        for (declaration in declarations) {
            if (declaration.getContainingJetFile().isCompiled()) {
                //TODO: it's temporary while resolveToDescriptor does not work for compiled declarations
                for (descriptor in resolutionFacade.resolveImportReference(moduleDescriptor, declaration.getFqName()!!)) {
                    if (descriptor is CallableDescriptor && descriptor.getExtensionReceiverParameter() != null) {
                        processDescriptor(descriptor)
                    }
                }
            }
            else {
                processDescriptor(resolutionFacade.resolveToDescriptor(declaration) as CallableDescriptor)
            }
        }

        return result
    }

    public fun getJvmClassesByName(name: String): Collection<ClassifierDescriptor>
            = PsiShortNamesCache.getInstance(project).getClassesByName(name, scope)
            .map { resolutionFacade.psiClassToDescriptor(it) }
            .filterNotNull()
            .filter(descriptorFilter)
            .toSet()

    public fun getKotlinClasses(nameFilter: (String) -> Boolean, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        return JetFullClassNameIndex.getInstance().getAllKeys(project).asSequence()
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toList()
                .flatMap { getClassDescriptorsByFQName(it, kindFilter) }
    }

    private fun getClassDescriptorsByFQName(classFQName: FqName, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        val declarations = JetFullClassNameIndex.getInstance()[classFQName.asString(), project, scope]

        if (declarations.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return emptyList()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(moduleDescriptor, classFQName) { kindFilter(it.getKind()) }
                .filter(descriptorFilter)
    }

    private fun findTopLevelCallables(fqName: FqName): Collection<CallableDescriptor> {
        return resolutionFacade.resolveImportReference(moduleDescriptor, fqName)
                .filterIsInstance<CallableDescriptor>()
                .filter { it.getExtensionReceiverParameter() == null }
    }

    private fun isExcludedFromAutoImport(descriptor: DeclarationDescriptor): Boolean {
        val fqName = descriptor.importableFqName?.asString() ?: return false

        return CodeInsightSettings.getInstance().EXCLUDED_PACKAGES
                .any { excluded -> fqName == excluded || (fqName.startsWith(excluded) && fqName[excluded.length()] == '.') }
    }
}

