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

package org.jetbrains.kotlin.idea.core

import com.intellij.codeInsight.JavaProjectCodeInsightSettings
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.util.Processor
import com.intellij.util.indexing.IdFilter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getJavaFieldDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.receiverTypes
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.SamAdapterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.isHiddenInResolution
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

class KotlinIndicesHelper(
        private val resolutionFacade: ResolutionFacade,
        private val scope: GlobalSearchScope,
        visibilityFilter: (DeclarationDescriptor) -> Boolean,
        private val declarationTranslator: (KtDeclaration) -> KtDeclaration? = { it },
        applyExcludeSettings: Boolean = true,
        private val filterOutPrivate: Boolean = true
) {

    private val moduleDescriptor = resolutionFacade.moduleDescriptor
    private val project = resolutionFacade.project

    private val descriptorFilter: (DeclarationDescriptor) -> Boolean = filter@ {
        if (it.isHiddenInResolution()) return@filter false
        if (!visibilityFilter(it)) return@filter false
        if (applyExcludeSettings && isExcludedFromAutoImport(it)) return@filter false
        true
    }

    fun getTopLevelCallablesByName(name: String): Collection<CallableDescriptor> {
        val declarations = LinkedHashSet<KtCallableDeclaration>()
        declarations.addTopLevelNonExtensionCallablesByName(KotlinFunctionShortNameIndex.getInstance(), name)
        declarations.addTopLevelNonExtensionCallablesByName(KotlinPropertyShortNameIndex.getInstance(), name)
        return declarations
                .flatMap { it.resolveToDescriptorsWithHack() }
                .filter { descriptorFilter(it) }
    }

    private fun MutableSet<KtCallableDeclaration>.addTopLevelNonExtensionCallablesByName(
            index: StringStubIndexExtension<out KtCallableDeclaration>,
            name: String
    ) {
        index.get(name, project, scope).filterTo(this) { it.parent is KtFile && it.receiverTypeReference == null }
    }

    fun getTopLevelExtensionOperatorsByName(name: String): Collection<FunctionDescriptor> {
        return KotlinFunctionShortNameIndex.getInstance().get(name, project, scope)
                .filter { it.parent is KtFile && it.receiverTypeReference != null && it.hasModifier(KtTokens.OPERATOR_KEYWORD) }
                .flatMap { it.resolveToDescriptorsWithHack() }
                .filterIsInstance<FunctionDescriptor>()
                .filter { descriptorFilter(it) && it.extensionReceiverParameter != null }
                .distinct()
    }

    fun getMemberOperatorsByName(name: String): Collection<FunctionDescriptor> {
        return KotlinFunctionShortNameIndex.getInstance().get(name, project, scope)
                .filter { it.parent is KtClassBody && it.receiverTypeReference == null && it.hasModifier(KtTokens.OPERATOR_KEYWORD) }
                .flatMap { it.resolveToDescriptorsWithHack() }
                .filterIsInstance<FunctionDescriptor>()
                .filter { descriptorFilter(it) && it.extensionReceiverParameter == null }
                .distinct()
    }

    fun processTopLevelCallables(nameFilter: (String) -> Boolean, processor: (CallableDescriptor) -> Unit) {
        fun processIndex(index: StringStubIndexExtension<out KtCallableDeclaration>) {
            for (key in index.getAllKeys(project)) {
                ProgressManager.checkCanceled()
                if (!nameFilter(key.substringAfterLast('.', key))) continue

                for (declaration in index.get(key, project, scope)) {
                    if (declaration.receiverTypeReference != null) continue
                    if (filterOutPrivate && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) continue

                    for (descriptor in declaration.resolveToDescriptorsWithHack()) {
                        if (descriptorFilter(descriptor)) {
                            processor(descriptor)
                        }
                    }
                }
            }
        }
        processIndex(KotlinTopLevelFunctionFqnNameIndex.getInstance())
        processIndex(KotlinTopLevelPropertyFqnNameIndex.getInstance())
    }

    fun getCallableTopLevelExtensions(
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            position: KtExpression,
            bindingContext: BindingContext,
            nameFilter: (String) -> Boolean
    ): Collection<CallableDescriptor> {
        val receiverTypes = callTypeAndReceiver.receiverTypes(bindingContext, position, moduleDescriptor, resolutionFacade, predictableSmartCastsOnly = false)
                            ?: return emptyList()
        return getCallableTopLevelExtensions(callTypeAndReceiver, receiverTypes, nameFilter)
    }

    fun getCallableTopLevelExtensions(
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            receiverTypes: Collection<KotlinType>,
            nameFilter: (String) -> Boolean
    ): Collection<CallableDescriptor> {
        if (receiverTypes.isEmpty()) return emptyList()

        val receiverTypeNames = HashSet<String>()
        receiverTypes.forEach { receiverTypeNames.addTypeNames(it) }

        val index = KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE

        val declarations = index.getAllKeys(project)
                .asSequence()
                .filter {
                    ProgressManager.checkCanceled()
                    KotlinTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames
                        && nameFilter(KotlinTopLevelExtensionsByReceiverTypeIndex.callableNameFromKey(it))
                }
                .flatMap { index.get(it, project, scope).asSequence() }

        val suitableExtensions = findSuitableExtensions(declarations, receiverTypes, callTypeAndReceiver.callType)

        val additionalDescriptors = ArrayList<CallableDescriptor>(0)

        for (extension in KotlinIndicesHelperExtension.getInstances(project)) {
            extension.appendExtensionCallables(additionalDescriptors, moduleDescriptor, receiverTypes, nameFilter)
        }

        return if (additionalDescriptors.isNotEmpty())
            suitableExtensions + additionalDescriptors
        else
            suitableExtensions
    }

    private fun MutableCollection<String>.addTypeNames(type: KotlinType) {
        val constructor = type.constructor
        addIfNotNull(constructor.declarationDescriptor?.name?.asString())
        constructor.supertypes.forEach { addTypeNames(it) }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(
            declarations: Sequence<KtCallableDeclaration>,
            receiverTypes: Collection<KotlinType>,
            callType: CallType<*>
    ): Collection<CallableDescriptor> {
        val result = LinkedHashSet<CallableDescriptor>()

        fun processDescriptor(descriptor: CallableDescriptor) {
            if (descriptor.extensionReceiverParameter != null && descriptorFilter(descriptor)) {
                result.addAll(descriptor.substituteExtensionIfCallable(receiverTypes, callType))
            }
        }

        declarations.forEach { it.resolveToDescriptorsWithHack().forEach(::processDescriptor) }

        return result
    }

    fun getJvmClassesByName(name: String): Collection<ClassDescriptor> {
        return PsiShortNamesCache.getInstance(project).getClassesByName(name, scope)
                .mapNotNull { it.resolveToDescriptor(resolutionFacade) }
                .filter(descriptorFilter)
                .toSet()
    }

    fun getKotlinClasses(nameFilter: (String) -> Boolean, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        return KotlinFullClassNameIndex.getInstance().getAllKeys(project).asSequence()
                .map { FqName(it) }
                .filter {
                    ProgressManager.checkCanceled()
                    nameFilter(it.shortName().asString())
                }
                .toList()
                .flatMap { getClassDescriptorsByFQName(it, kindFilter) }
    }

    private fun getClassDescriptorsByFQName(classFQName: FqName, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        val declarations = KotlinFullClassNameIndex.getInstance()[classFQName.asString(), project, scope]

        if (declarations.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return emptyList()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(moduleDescriptor, classFQName) { kindFilter(it!!.kind) }
                .filter(descriptorFilter)
    }

    fun processObjectMembers(
            descriptorKindFilter: DescriptorKindFilter,
            nameFilter: (String) -> Boolean,
            filter: (KtCallableDeclaration, KtObjectDeclaration) -> Boolean,
            processor: (DeclarationDescriptor) -> Unit
    ) {
        fun processIndex(index: StringStubIndexExtension<out KtCallableDeclaration>) {
            for (name in index.getAllKeys(project)) {
                ProgressManager.checkCanceled()
                if (!nameFilter(name)) continue

                for (declaration in index.get(name, project, scope)) {
                    val objectDeclaration = declaration.parent.parent as? KtObjectDeclaration ?: continue
                    if (objectDeclaration.isObjectLiteral()) continue
                    if (filterOutPrivate && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) continue
                    if (!filter(declaration, objectDeclaration)) continue
                    for (descriptor in declaration.resolveToDescriptorsWithHack()) {
                        if (descriptorKindFilter.accepts(descriptor) && descriptorFilter(descriptor)) {
                            processor(descriptor)
                        }
                    }
                }
            }
        }

        if (descriptorKindFilter.kindMask.and(DescriptorKindFilter.FUNCTIONS_MASK) != 0) {
            processIndex(KotlinFunctionShortNameIndex.getInstance())
        }
        if (descriptorKindFilter.kindMask.and(DescriptorKindFilter.VARIABLES_MASK) != 0) {
            processIndex(KotlinPropertyShortNameIndex.getInstance())
        }
    }

    fun processJavaStaticMembers(
            descriptorKindFilter: DescriptorKindFilter,
            nameFilter: (String) -> Boolean,
            processor: (DeclarationDescriptor) -> Unit
    ) {
        val idFilter = IdFilter.getProjectIdFilter(resolutionFacade.project, false)
        val shortNamesCache = PsiShortNamesCache.getInstance(project)

        val methodNamesProcessor = Processor<String> { name ->
            ProgressManager.checkCanceled()
            if (!nameFilter(name)) return@Processor true

            for (method in shortNamesCache.getMethodsByName(name, scope)) {
                if (!method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (filterOutPrivate && method.hasModifierProperty(PsiModifier.PRIVATE)) continue
                if (method.containingClass?.parent !is PsiFile) continue // only top-level classes
                val descriptor = method.getJavaMethodDescriptor(resolutionFacade) ?: continue
                val container = descriptor.containingDeclaration as? ClassDescriptor ?: continue
                if (descriptorKindFilter.accepts(descriptor) && descriptorFilter(descriptor)) {
                    processor(descriptor)

                    // SAM-adapter
                    container.staticScope.getContributedFunctions(descriptor.name, NoLookupLocation.FROM_IDE)
                            .filterIsInstance<SamAdapterDescriptor<*>>()
                            .firstOrNull { it.baseDescriptorForSynthetic.original == descriptor.original }
                            ?.let { processor(it) }
                }
            }
            true
        }
        shortNamesCache.processAllMethodNames(methodNamesProcessor, scope, idFilter)

        val fieldNamesProcessor = Processor<String> { name ->
            ProgressManager.checkCanceled()
            if (!nameFilter(name)) return@Processor true

            for (field in shortNamesCache.getFieldsByName(name, scope)) {
                if (!field.hasModifierProperty(PsiModifier.STATIC)) continue
                if (filterOutPrivate && field.hasModifierProperty(PsiModifier.PRIVATE)) continue
                val descriptor = field.getJavaFieldDescriptor() ?: continue
                if (descriptorKindFilter.accepts(descriptor) && descriptorFilter(descriptor)) {
                    processor(descriptor)
                }
            }
            true
        }
        shortNamesCache.processAllFieldNames(fieldNamesProcessor, scope, idFilter)
    }

    private fun isExcludedFromAutoImport(descriptor: DeclarationDescriptor): Boolean {
        val fqName = descriptor.importableFqName?.asString() ?: return false
        return JavaProjectCodeInsightSettings.getSettings(project).isExcluded(fqName)
    }

    private fun KtCallableDeclaration.resolveToDescriptorsWithHack(): Collection<CallableDescriptor> {
        if (getContainingKtFile().isCompiled) { //TODO: it's temporary while resolveToDescriptor does not work for compiled declarations
            return resolutionFacade.resolveImportReference(moduleDescriptor, fqName!!).filterIsInstance<CallableDescriptor>()
        }
        else {
            val translatedDeclaration = declarationTranslator(this) ?: return emptyList()
            return (resolutionFacade.resolveToDescriptor(translatedDeclaration) as? CallableDescriptor).singletonOrEmptyList()
        }
    }
}

